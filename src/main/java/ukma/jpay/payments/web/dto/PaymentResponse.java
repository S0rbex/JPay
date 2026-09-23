package ukma.jpay.payments.web.dto;

import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        TransactionStatus status,
        BigDecimal amount,
        String currency,
        String merchantReference,
        Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.id(),
                payment.status(),
                payment.amount(),
                payment.currency(),
                payment.merchantReference(),
                payment.createdAt());
    }
}
