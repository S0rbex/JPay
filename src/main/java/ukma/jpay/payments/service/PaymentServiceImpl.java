package ukma.jpay.payments.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.PaymentStatusChanged;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.error.PaymentNotFoundException;
import ukma.jpay.payments.error.UnsupportedCurrencyException;
import ukma.jpay.payments.provider.PaymentProviderClient;
import ukma.jpay.payments.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final List<PaymentProviderClient> providerClients;
    private final ApplicationEventPublisher eventPublisher;

    PaymentServiceImpl(
            PaymentRepository paymentRepository,
            List<PaymentProviderClient> providerClients,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.providerClients = providerClients;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Payment create(BigDecimal amount, String currency, String merchantReference) {
        PaymentProviderClient provider = providerClients.stream()
                .filter(client -> client.supports(currency))
                .findFirst()
                .orElseThrow(() -> new UnsupportedCurrencyException(currency));

        Payment payment = new Payment(
                UUID.randomUUID(),
                TransactionStatus.INITIATED,
                amount,
                currency,
                merchantReference,
                provider.providerId(),
                Instant.now());

        provider.submit(payment);

        Payment processing = payment.transitionTo(TransactionStatus.PROCESSING);
        paymentRepository.save(processing);
        eventPublisher.publishEvent(new PaymentStatusChanged(
                processing.id(), TransactionStatus.INITIATED, TransactionStatus.PROCESSING));

        return processing;
    }

    @Override
    public Payment get(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    @Override
    @Transactional
    public void updateStatus(UUID paymentId, TransactionStatus status) {
        Payment current = get(paymentId);
        Payment updated = current.transitionTo(status);
        paymentRepository.save(updated);
        eventPublisher.publishEvent(new PaymentStatusChanged(paymentId, current.status(), status));
    }
}
