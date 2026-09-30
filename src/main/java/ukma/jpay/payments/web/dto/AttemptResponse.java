package ukma.jpay.payments.web.dto;

import ukma.jpay.payments.domain.PaymentAttempt;
import ukma.jpay.payments.persistence.AttemptResult;

import java.time.Instant;
import java.util.UUID;

public record AttemptResponse(
        UUID id,
        UUID paymentId,
        String providerId,
        int attemptNumber,
        AttemptResult result,
        Instant createdAt) {

    public static AttemptResponse from(PaymentAttempt attempt) {
        return new AttemptResponse(attempt.id(), attempt.paymentId(), attempt.providerId(),
                attempt.attemptNumber(), attempt.result(), attempt.createdAt());
    }
}
