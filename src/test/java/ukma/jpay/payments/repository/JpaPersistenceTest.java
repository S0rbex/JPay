package ukma.jpay.payments.repository;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.PaymentDetails;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.persistence.AttemptResult;
import ukma.jpay.payments.persistence.MerchantEntity;
import ukma.jpay.payments.persistence.PaymentAttemptEntity;
import ukma.jpay.payments.persistence.PaymentEntity;
import ukma.jpay.payments.persistence.PaymentProviderEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
@Import(JpaPaymentStore.class)
class JpaPersistenceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JpaPaymentRepository payments;
    @Autowired
    private MerchantRepository merchants;
    @Autowired
    private PaymentProviderRepository providers;
    @Autowired
    private PaymentAttemptRepository attempts;
    @Autowired
    private PaymentRepository store;

    @Test
    void existingPaymentContractCanSaveReadAndUpdate() {
        var processing = payment("order-1", "stripe");
        store.save(processing);
        flushAndClear();
        assertThat(store.findById(processing.id())).contains(processing);

        var succeeded = processing.transitionTo(TransactionStatus.SUCCEEDED);
        store.save(succeeded);
        flushAndClear();
        assertThat(store.findById(processing.id())).contains(succeeded);
        assertThat(payments.findById(processing.id()).orElseThrow().getVersion()).isEqualTo(1L);
    }

    @Test
    void missingPaymentReturnsEmptyOptional() {
        assertThat(store.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void atomicStatusUpdateChecksExpectedStateAndRefreshesPersistenceContext() {
        var processing = payment("atomic-order", "stripe");
        store.save(processing);
        flushAndClear();
        assertThat(store.findById(processing.id())).contains(processing);

        assertThat(store.updateStatus(processing.id(), TransactionStatus.PROCESSING, TransactionStatus.SUCCEEDED))
                .isTrue();
        assertThat(store.findById(processing.id())).contains(processing.transitionTo(TransactionStatus.SUCCEEDED));
        assertThat(payments.findById(processing.id()).orElseThrow().getVersion()).isEqualTo(1L);

        assertThat(store.updateStatus(processing.id(), TransactionStatus.PROCESSING, TransactionStatus.FAILED))
                .isFalse();
        assertThat(store.findById(processing.id())).contains(processing.transitionTo(TransactionStatus.SUCCEEDED));
        assertThat(payments.findById(processing.id()).orElseThrow().getVersion()).isEqualTo(1L);
        assertThat(store.updateStatus(UUID.randomUUID(), TransactionStatus.PROCESSING, TransactionStatus.SUCCEEDED))
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void paymentDtoCollectionUsesOneQueryRegardlessOfNumberOfPayments(int paymentCount) {
        for (int index = 0; index < paymentCount; index++) {
            var primary = providers.save(new PaymentProviderEntity("primary-" + index, "Primary " + index, 1));
            var fallback = providers.save(new PaymentProviderEntity("fallback-" + index, "Fallback " + index, 2));
            var merchant = merchants.save(new MerchantEntity("Shop " + index, "shop" + index + "@example.com", "USD"));
            var entity = new PaymentEntity(payment("order-" + index, primary.getId()), primary);
            entity.setMerchant(merchant);
            entity.addAttempt(new PaymentAttemptEntity(primary, 2, AttemptResult.SUBMITTED, CREATED_AT.plusSeconds(2)));
            entity.addAttempt(new PaymentAttemptEntity(fallback, 1, AttemptResult.FAILED, CREATED_AT.plusSeconds(1)));
            payments.save(entity);
        }
        var noAttempts = savePaymentWithAttempts(0);
        flushAndClear();
        var statistics = resetStatistics();

        var result = store.findAllWithDetails();
        entityManager.clear();

        assertThat(result).hasSize(paymentCount + 1);
        assertThat(result).extracting(details -> details.payment().id()).doesNotHaveDuplicates();
        assertThat(result).filteredOn(details -> details.payment().id().equals(noAttempts.getId()))
                .singleElement().satisfies(details -> {
                    assertThat(details.merchant()).isNull();
                    assertThat(details.attempts()).isEmpty();
                });
        assertThat(result).filteredOn(details -> !details.payment().id().equals(noAttempts.getId()))
                .allSatisfy(details -> {
                    assertThat(details.merchant().name()).startsWith("Shop ");
                    assertThat(details.provider().name()).startsWith("Primary ");
                    assertThat(details.attempts()).extracting(PaymentDetails.AttemptSummary::attemptNumber)
                            .containsExactly(1, 2);
                    assertThat(details.attempts().getFirst().provider().name()).startsWith("Fallback ");
                    assertThat(details.attempts().getLast().provider().name()).startsWith("Primary ");
                });
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void emptyPaymentCollectionStillUsesOneQuery() {
        flushAndClear();
        var statistics = resetStatistics();
        assertThat(store.findAllWithDetails()).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void merchantCollectionFetchesAllProvidersInOneQueryIncludingEmptyCollections() {
        var merchant = new MerchantEntity("Shop", "shop@example.com", "USD");
        merchant.enableProvider(providers.getReferenceById("stripe"));
        merchant.enableProvider(providers.getReferenceById("liqpay"));
        merchants.save(merchant);
        merchants.save(new MerchantEntity("Empty shop", "empty@example.com", "EUR"));
        flushAndClear();
        var statistics = resetStatistics();

        var result = merchants.findAllWithProviders();
        entityManager.clear();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getProviders()).isEmpty();
        assertThat(result.getLast().getProviders()).extracting(PaymentProviderEntity::getName)
                .containsExactlyInAnyOrder("Stripe", "LiqPay");
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void attemptsFetchProvidersAndPaymentInOneQuery() {
        var payment = savePaymentWithAttempts(2);
        flushAndClear();
        var statistics = resetStatistics();

        var result = attempts.findWithDetailsByPaymentId(payment.getId());
        entityManager.clear();

        assertThat(result).hasSize(2).allSatisfy(attempt -> {
            assertThat(attempt.getProvider().getName()).isEqualTo("Stripe");
            assertThat(attempt.getPayment().toPayment().merchantReference()).isEqualTo("order");
        });
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void removingAttemptFromParentDeletesOnlyThatOrphan() {
        var payment = savePaymentWithAttempts(2);
        flushAndClear();
        var loaded = payments.findWithDetailsById(payment.getId()).orElseThrow();
        var removedId = loaded.getAttempts().getFirst().getId();
        var retainedId = loaded.getAttempts().getLast().getId();

        loaded.removeAttempt(loaded.getAttempts().getFirst());
        flushAndClear();

        assertThat(attempts.findById(removedId)).isEmpty();
        assertThat(attempts.findById(retainedId)).isPresent();
        assertThat(payments.findById(payment.getId())).isPresent();
        assertThat(providers.findById("stripe")).isPresent();
    }

    @Test
    void deletingPaymentCascadesToAttemptsAndPreservesSharedReferences() {
        var merchant = merchants.save(new MerchantEntity("Shop", "shop@example.com", "USD"));
        var payment = savePaymentWithAttempts(2);
        payment.setMerchant(merchant);
        flushAndClear();
        assertThat(attempts.countByPaymentId(payment.getId())).isEqualTo(2);

        payments.deleteById(payment.getId());
        flushAndClear();

        assertThat(attempts.countByPaymentId(payment.getId())).isZero();
        assertThat(merchants.findById(merchant.getId())).isPresent();
        assertThat(providers.findById("stripe")).isPresent();
    }

    @Test
    void deletingUnreferencedMerchantRemovesLinksButKeepsProviders() {
        var merchant = new MerchantEntity("Shop", "shop@example.com", "USD");
        merchant.enableProvider(providers.getReferenceById("stripe"));
        merchants.saveAndFlush(merchant);
        entityManager.clear();

        merchants.deleteById(merchant.getId());
        flushAndClear();

        assertThat(merchants.findById(merchant.getId())).isEmpty();
        assertThat(providers.findById("stripe")).isPresent();
    }

    @Test
    void repositoriesSupportCrudAndNamedAndCustomQueries() {
        var provider = providers.save(new PaymentProviderEntity("test-provider", "Test Provider", 3));
        var merchant = new MerchantEntity("Old shop", "SHOP@example.com", "USD");
        merchant.enableProvider(provider);
        merchants.save(merchant);
        var payment = new PaymentEntity(payment("crud-order", provider.getId()), provider);
        payment.setMerchant(merchant);
        payment.addAttempt(new PaymentAttemptEntity(provider, 1, AttemptResult.SUBMITTED, CREATED_AT));
        payments.save(payment);
        flushAndClear();

        var foundMerchant = merchants.findByContactEmailIgnoreCase("SHOP@EXAMPLE.COM").orElseThrow();
        assertThat(merchants.existsByContactEmailIgnoreCase("shop@example.com")).isTrue();
        foundMerchant.update("New shop", "shop@example.com", "EUR");
        var foundProvider = providers.findByNameContainingIgnoreCaseOrderByIdAsc("TEST").getFirst();
        foundProvider.update("Updated Provider", 4);
        var foundPayment = payments.findByStatusAndCurrencyOrderByCreatedAtAscIdAsc(TransactionStatus.PROCESSING, "USD")
                .getFirst();
        foundPayment.updateFrom(foundPayment.toPayment().transitionTo(TransactionStatus.SUCCEEDED), foundProvider);
        assertThat(attempts.existsByPaymentIdAndAttemptNumber(payment.getId(), 1)).isTrue();
        var attempt = attempts.findWithDetailsByPaymentId(payment.getId()).getFirst();
        attempt.setResult(AttemptResult.SUCCEEDED);
        flushAndClear();

        assertThat(merchants.findWithProvidersById(merchant.getId()).orElseThrow().getName()).isEqualTo("New shop");
        assertThat(providers.findEnabledForMerchant(merchant.getId())).singleElement()
                .satisfies(value -> assertThat(value.getName()).isEqualTo("Updated Provider"));
        assertThat(payments.findWithDetailsById(payment.getId()).orElseThrow().toPayment().status())
                .isEqualTo(TransactionStatus.SUCCEEDED);
        assertThat(attempts.findById(attempt.getId()).orElseThrow().getResult()).isEqualTo(AttemptResult.SUCCEEDED);

        var parent = payments.findWithDetailsById(payment.getId()).orElseThrow();
        parent.removeAttempt(parent.getAttempts().getFirst());
        flushAndClear();
        assertThat(attempts.findById(attempt.getId())).isEmpty();
        payments.deleteById(payment.getId());
        payments.flush();
        merchants.deleteById(merchant.getId());
        merchants.flush();
        providers.deleteById(provider.getId());
        flushAndClear();
        assertThat(payments.findById(payment.getId())).isEmpty();
        assertThat(merchants.findById(merchant.getId())).isEmpty();
        assertThat(providers.findById(provider.getId())).isEmpty();
    }

    @Test
    void duplicateAttemptNumbersAreRejectedByDatabase() {
        var payment = savePaymentWithAttempts(1);
        payment.addAttempt(new PaymentAttemptEntity(providers.getReferenceById("stripe"),
                1, AttemptResult.FAILED, CREATED_AT));
        assertThatThrownBy(() -> payments.saveAndFlush(payment)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateMerchantEmailsAreRejectedByDatabase() {
        merchants.saveAndFlush(new MerchantEntity("First", "shop@example.com", "USD"));
        assertThatThrownBy(() -> merchants.saveAndFlush(new MerchantEntity("Second", "SHOP@example.com", "EUR")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void providerReferencedByPaymentCannotBeDeleted() {
        savePaymentWithAttempts(0);
        flushAndClear();
        providers.deleteById("stripe");
        assertThatThrownBy(providers::flush).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void merchantReferencedByPaymentCannotBeDeleted() {
        var merchant = merchants.save(new MerchantEntity("Shop", "shop@example.com", "USD"));
        var payment = savePaymentWithAttempts(0);
        payment.setMerchant(merchant);
        flushAndClear();
        merchants.deleteById(merchant.getId());
        assertThatThrownBy(merchants::flush).isInstanceOf(DataIntegrityViolationException.class);
    }

    private PaymentEntity savePaymentWithAttempts(int count) {
        var provider = providers.getReferenceById("stripe");
        var payment = new PaymentEntity(payment("order", "stripe"), provider);
        for (int number = 1; number <= count; number++) {
            payment.addAttempt(new PaymentAttemptEntity(provider, number, AttemptResult.SUBMITTED, CREATED_AT));
        }
        return payments.save(payment);
    }

    private static Payment payment(String reference, String providerId) {
        return new Payment(UUID.randomUUID(), TransactionStatus.PROCESSING,
                new BigDecimal("19.99"), "USD", reference, providerId, CREATED_AT);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Statistics resetStatistics() {
        var statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }
}
