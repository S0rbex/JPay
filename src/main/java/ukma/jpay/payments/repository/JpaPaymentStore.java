package ukma.jpay.payments.repository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.PaymentDetails;
import ukma.jpay.payments.domain.PaymentDetails.AttemptSummary;
import ukma.jpay.payments.domain.PaymentDetails.MerchantSummary;
import ukma.jpay.payments.domain.PaymentDetails.ProviderSummary;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.persistence.AttemptResult;
import ukma.jpay.payments.persistence.PaymentAttemptEntity;
import ukma.jpay.payments.persistence.PaymentEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
class JpaPaymentStore implements PaymentRepository {

    private final JpaPaymentRepository payments;
    private final PaymentProviderRepository providers;
    private final MerchantRepository merchants;

    JpaPaymentStore(JpaPaymentRepository payments, PaymentProviderRepository providers,
                    MerchantRepository merchants) {
        this.payments = payments;
        this.providers = providers;
        this.merchants = merchants;
    }

    @Override
    @Transactional
    public Payment create(Payment payment, UUID merchantId) {
        var provider = providers.getReferenceById(payment.providerId());
        var entity = new PaymentEntity(payment, provider);
        entity.setMerchant(merchants.getReferenceById(merchantId));
        entity.addAttempt(new PaymentAttemptEntity(
                provider, 1, AttemptResult.SUBMITTED, payment.createdAt()));
        return payments.save(entity).toPayment();
    }

    @Override
    @Transactional
    public void deleteById(UUID paymentId) {
        payments.deleteById(paymentId);
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        var provider = providers.getReferenceById(payment.providerId());
        var entity = payments.findById(payment.id())
                .orElseGet(() -> new PaymentEntity(payment, provider));
        entity.updateFrom(payment, provider);
        return payments.save(entity).toPayment();
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return payments.findWithDetailsById(paymentId).map(PaymentEntity::toPayment);
    }

    @Override
    @Transactional
    public boolean updateStatus(UUID paymentId, TransactionStatus from, TransactionStatus to) {
        return payments.updateStatusIfCurrent(paymentId, from, to) == 1;
    }

    @Override
    public List<PaymentDetails> findAllWithDetails() {
        return payments.findAllWithDetails().stream().map(JpaPaymentStore::toDetails).toList();
    }

    private static PaymentDetails toDetails(PaymentEntity entity) {
        var merchant = entity.getMerchant();
        var provider = entity.getProvider();
        var attempts = entity.getAttempts().stream()
                .map(attempt -> new AttemptSummary(
                        attempt.getId(), attempt.getAttemptNumber(), attempt.getResult().name(),
                        new ProviderSummary(attempt.getProvider().getId(), attempt.getProvider().getName()),
                        attempt.getCreatedAt()))
                .toList();
        return new PaymentDetails(entity.toPayment(),
                merchant == null ? null : new MerchantSummary(merchant.getId(), merchant.getName()),
                new ProviderSummary(provider.getId(), provider.getName()), attempts);
    }
}
