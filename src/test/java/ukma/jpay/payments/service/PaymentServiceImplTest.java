package ukma.jpay.payments.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.PaymentStatusChanged;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.error.InvalidStateTransitionException;
import ukma.jpay.payments.error.MerchantNotFoundException;
import ukma.jpay.payments.error.PaymentNotFoundException;
import ukma.jpay.payments.error.ProviderMismatchException;
import ukma.jpay.payments.error.UnsupportedCurrencyException;
import ukma.jpay.payments.persistence.PaymentProviderEntity;
import ukma.jpay.payments.provider.PaymentProviderClient;
import ukma.jpay.payments.repository.MerchantRepository;
import ukma.jpay.payments.repository.PaymentProviderRepository;
import ukma.jpay.payments.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final UUID MERCHANT_ID = UUID.randomUUID();

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private PaymentProviderRepository providerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PaymentProviderClient stripe;
    private PaymentProviderClient liqpay;

    private PaymentServiceImpl service(PaymentProviderClient... providers) {
        return new PaymentServiceImpl(
                paymentRepository, merchantRepository, providerRepository, List.of(providers), eventPublisher);
    }

    @Test
    void createRoutesToFirstSupportingProvider() {
        stripe = fakeProvider("stripe", "USD");
        liqpay = fakeProvider("liqpay", "USD", "UAH");
        merchantEnables("stripe", "liqpay");
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = service(stripe, liqpay).create(MERCHANT_ID, new BigDecimal("10.00"), "USD", "order-1");

        assertThat(payment.providerId()).isEqualTo("stripe");
        assertThat(payment.status()).isEqualTo(TransactionStatus.PROCESSING);
        verify(paymentRepository).create(any(), eq(MERCHANT_ID));
        verify(paymentRepository).save(payment);
        verify(eventPublisher).publishEvent(
                new PaymentStatusChanged(payment.id(), TransactionStatus.INITIATED, TransactionStatus.PROCESSING));
    }

    @Test
    void createFallsBackToSecondProviderWhenFirstDoesNotSupportCurrency() {
        stripe = fakeProvider("stripe", "USD");
        liqpay = fakeProvider("liqpay", "UAH");
        merchantEnables("stripe", "liqpay");
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = service(stripe, liqpay).create(MERCHANT_ID, new BigDecimal("10.00"), "UAH", "order-1");

        assertThat(payment.providerId()).isEqualTo("liqpay");
    }

    @Test
    void createOnlyRoutesToProvidersEnabledForMerchant() {
        stripe = fakeProvider("stripe", "USD");
        liqpay = fakeProvider("liqpay", "USD");
        merchantEnables("liqpay");
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = service(stripe, liqpay).create(MERCHANT_ID, new BigDecimal("10.00"), "USD", "order-1");

        assertThat(payment.providerId()).isEqualTo("liqpay");
    }

    @Test
    void createForMissingMerchantThrowsAndSavesNothing() {
        when(merchantRepository.existsById(MERCHANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service(fakeProvider("stripe", "USD"))
                .create(MERCHANT_ID, new BigDecimal("10.00"), "USD", "order-1"))
                .isInstanceOf(MerchantNotFoundException.class);

        verifyNoInteractions(paymentRepository, eventPublisher);
    }

    @Test
    void createWithUnsupportedCurrencyThrowsAndSavesNothing() {
        stripe = fakeProvider("stripe", "USD");
        merchantEnables("stripe");

        assertThatThrownBy(() -> service(stripe).create(MERCHANT_ID, new BigDecimal("10.00"), "GBP", "order-1"))
                .isInstanceOf(UnsupportedCurrencyException.class);

        verifyNoInteractions(paymentRepository, eventPublisher);
    }

    @Test
    void getOnMissingPaymentThrows() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(paymentId))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void updateStatusWithValidTransitionSavesAndPublishes() {
        Payment processing = processingPayment();
        when(paymentRepository.findById(processing.id())).thenReturn(Optional.of(processing));
        when(paymentRepository.updateStatus(
                processing.id(), TransactionStatus.PROCESSING, TransactionStatus.SUCCEEDED))
                .thenReturn(true);

        service().updateStatus(processing.id(), "stripe", TransactionStatus.SUCCEEDED);

        verify(paymentRepository).updateStatus(
                processing.id(), TransactionStatus.PROCESSING, TransactionStatus.SUCCEEDED);
        verify(eventPublisher).publishEvent(new PaymentStatusChanged(
                processing.id(), TransactionStatus.PROCESSING, TransactionStatus.SUCCEEDED));
    }

    @Test
    void updateStatusWithInvalidTransitionThrowsAndSavesNothing() {
        Payment processing = processingPayment();
        when(paymentRepository.findById(processing.id())).thenReturn(Optional.of(processing));

        assertThatThrownBy(() -> service().updateStatus(processing.id(), "stripe", TransactionStatus.REFUNDED))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void updateStatusFromDifferentProviderThrowsAndSavesNothing() {
        Payment processing = processingPayment();
        when(paymentRepository.findById(processing.id())).thenReturn(Optional.of(processing));

        assertThatThrownBy(() -> service().updateStatus(
                processing.id(), "liqpay", TransactionStatus.SUCCEEDED))
                .isInstanceOf(ProviderMismatchException.class);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void repeatedWebhookDoesNotWriteOrPublishAnotherEvent() {
        var succeeded = processingPayment().transitionTo(TransactionStatus.SUCCEEDED);
        when(paymentRepository.findById(succeeded.id())).thenReturn(Optional.of(succeeded));

        service().updateStatus(succeeded.id(), "stripe", TransactionStatus.SUCCEEDED);

        verify(paymentRepository, never()).updateStatus(any(), any(), any());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void concurrentDuplicateWebhookDoesNotPublishAnotherEvent() {
        var processing = processingPayment();
        var succeeded = processing.transitionTo(TransactionStatus.SUCCEEDED);
        when(paymentRepository.findById(processing.id()))
                .thenReturn(Optional.of(processing), Optional.of(succeeded));

        service().updateStatus(processing.id(), "stripe", TransactionStatus.SUCCEEDED);

        verify(paymentRepository).updateStatus(processing.id(), TransactionStatus.PROCESSING, TransactionStatus.SUCCEEDED);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void concurrentConflictingWebhookIsRejected() {
        var processing = processingPayment();
        var failed = processing.transitionTo(TransactionStatus.FAILED);
        when(paymentRepository.findById(processing.id()))
                .thenReturn(Optional.of(processing), Optional.of(failed));

        assertThatThrownBy(() -> service().updateStatus(processing.id(), "stripe", TransactionStatus.SUCCEEDED))
                .isInstanceOf(InvalidStateTransitionException.class);
        verifyNoInteractions(eventPublisher);
    }

    private void merchantEnables(String... providerIds) {
        when(merchantRepository.existsById(MERCHANT_ID)).thenReturn(true);
        var entities = new java.util.ArrayList<PaymentProviderEntity>();
        for (int i = 0; i < providerIds.length; i++) {
            entities.add(new PaymentProviderEntity(providerIds[i], providerIds[i], i + 1));
        }
        when(providerRepository.findEnabledForMerchant(MERCHANT_ID)).thenReturn(entities);
    }

    private static Payment processingPayment() {
        return new Payment(
                UUID.randomUUID(),
                TransactionStatus.PROCESSING,
                new BigDecimal("10.00"),
                "USD",
                "order-1",
                "stripe",
                java.time.Instant.now());
    }

    private static PaymentProviderClient fakeProvider(String id, String... currencies) {
        var supported = java.util.Set.of(currencies);
        return new PaymentProviderClient() {
            @Override
            public String providerId() {
                return id;
            }

            @Override
            public boolean supports(String currency) {
                return supported.contains(currency);
            }

            @Override
            public void submit(Payment payment) {
            }
        };
    }
}
