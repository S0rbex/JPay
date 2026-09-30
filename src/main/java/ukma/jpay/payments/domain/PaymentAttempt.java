package ukma.jpay.payments.domain;

import ukma.jpay.payments.persistence.AttemptResult;

import java.time.Instant;
import java.util.UUID;

public record PaymentAttempt(
        UUID id,
        UUID paymentId,
        String providerId,
        int attemptNumber,
        AttemptResult result,
        Instant createdAt) {
}
