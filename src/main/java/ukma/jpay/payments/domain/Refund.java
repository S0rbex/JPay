package ukma.jpay.payments.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Refund(
        UUID id,
        UUID paymentId,
        BigDecimal amount,
        String currency,
        RefundStatus status,
        String reason,
        Instant createdAt) {
}
