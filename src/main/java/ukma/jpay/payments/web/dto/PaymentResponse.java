package ukma.jpay.payments.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        @Schema(example = "83ec3956-d8e0-4c88-8147-70787150617a")
        UUID id,
        @Schema(example = "INITIATED")
        TransactionStatus status,
        @Schema(example = "19.99")
        BigDecimal amount,
        @Schema(example = "USD")
        String currency,
        @Schema(example = "order-1")
        String merchantReference,
        @Schema(example = "2026-10-06T12:00:00Z")
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
