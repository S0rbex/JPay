package ukma.jpay.payments.service;

import org.springframework.stereotype.Service;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.error.PaymentNotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryPaymentService implements PaymentService {

    private final Map<UUID, Payment> payments = new ConcurrentHashMap<>();

    @Override
    public Payment create(BigDecimal amount, String currency, String merchantReference) {
        Payment payment = new Payment(
                UUID.randomUUID(),
                TransactionStatus.INITIATED,
                amount,
                currency,
                merchantReference,
                Instant.now());
        payments.put(payment.id(), payment);
        return payment;
    }

    @Override
    public Payment get(UUID paymentId) {
        Payment payment = payments.get(paymentId);
        if (payment == null) {
            throw new PaymentNotFoundException(paymentId);
        }
        return payment;
    }

    @Override
    public void updateStatus(UUID paymentId, TransactionStatus status) {
        Payment current = get(paymentId);
        Payment updated = new Payment(
                current.id(),
                status,
                current.amount(),
                current.currency(),
                current.merchantReference(),
                current.createdAt());
        payments.put(paymentId, updated);
    }
}
