package ukma.jpay.payments.domain;

import java.time.Instant;
import java.util.UUID;

public record Attempt(
        UUID id,
        UUID paymentId,
        String providerId,
        int attemptNumber,
        AttemptResult result,
        String errorCode,
        long latencyMs,
        Instant startedAt) {
}
