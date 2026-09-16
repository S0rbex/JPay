package ukma.jpay.payments.service;

import org.springframework.stereotype.Service;
import ukma.jpay.merchants.domain.MerchantId;
import ukma.jpay.payments.domain.Attempt;
import ukma.jpay.payments.domain.AttemptResult;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.PaymentFailureDetail;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.error.PaymentNotFoundException;
import ukma.jpay.routing.domain.PaymentProvider;
import ukma.jpay.routing.service.ProviderRouter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class InMemoryPaymentService implements PaymentService {

    private final ProviderRouter providerRouter;
    private final Map<UUID, Payment> payments = new ConcurrentHashMap<>();
    private final Map<UUID, List<Attempt>> attemptsByPayment = new ConcurrentHashMap<>();

    public InMemoryPaymentService(ProviderRouter providerRouter) {
        this.providerRouter = providerRouter;
    }

    @Override
    public Payment create(MerchantId merchantId, CreatePaymentCommand command) {
        PaymentProvider provider = providerRouter.selectProvider(command.currency());

        UUID paymentId = UUID.randomUUID();
        Instant now = Instant.now();
        boolean declined = isSimulatedDecline(command);

        Attempt attempt = new Attempt(UUID.randomUUID(), paymentId, provider.id(), 1,
                declined ? AttemptResult.DECLINED : AttemptResult.SUCCESS,
                declined ? "card_declined" : null, 42L, now);
        attemptsByPayment.put(paymentId, new CopyOnWriteArrayList<>(List.of(attempt)));

        Payment payment = new Payment(
                paymentId,
                merchantId,
                declined ? TransactionStatus.FAILED : TransactionStatus.SUCCEEDED,
                command.amount(),
                command.currency(),
                provider.id(),
                provider.name(),
                command.merchantReference(),
                command.description(),
                declined
                        ? new PaymentFailureDetail("card_declined", "The provider declined the charge", "card_declined")
                        : null,
                BigDecimal.ZERO,
                now,
                now);

        payments.put(paymentId, payment);
        return payment;
    }

    @Override
    public Payment get(MerchantId merchantId, UUID paymentId) {
        Payment payment = payments.get(paymentId);
        if (payment == null || !payment.merchantId().equals(merchantId)) {
            throw new PaymentNotFoundException(paymentId);
        }
        return payment;
    }

    @Override
    public Optional<Payment> findById(UUID paymentId) {
        return Optional.ofNullable(payments.get(paymentId));
    }

    @Override
    public List<Attempt> listAttempts(MerchantId merchantId, UUID paymentId) {
        get(merchantId, paymentId);
        return List.copyOf(attemptsByPayment.getOrDefault(paymentId, List.of()));
    }

    @Override
    public Payment markRefunded(UUID paymentId, BigDecimal newRefundedAmount, TransactionStatus newStatus) {
        Payment current = payments.get(paymentId);
        Payment updated = new Payment(
                current.id(), current.merchantId(), newStatus, current.amount(), current.currency(),
                current.providerId(), current.providerName(), current.merchantReference(), current.description(),
                current.failure(), newRefundedAmount, current.createdAt(), Instant.now());
        payments.put(paymentId, updated);
        return updated;
    }

    private static boolean isSimulatedDecline(CreatePaymentCommand command) {
        return "decline-demo".equalsIgnoreCase(command.merchantReference());
    }
}
