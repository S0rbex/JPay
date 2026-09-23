package ukma.jpay.payments.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Payment(
        UUID id,
        TransactionStatus status,
        BigDecimal amount,
        String currency,
        String merchantReference,
        Instant createdAt) {
}
