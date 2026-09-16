package ukma.jpay.payments.web.dto;

import ukma.jpay.payments.domain.Attempt;
import ukma.jpay.payments.domain.AttemptResult;

import java.time.Instant;
import java.util.UUID;

public record AttemptResponse(
        UUID id,
        String providerId,
        int attemptNumber,
        AttemptResult result,
        String errorCode,
        long latencyMs,
        Instant startedAt) {

    public static AttemptResponse from(Attempt attempt) {
        return new AttemptResponse(attempt.id(), attempt.providerId(), attempt.attemptNumber(),
                attempt.result(), attempt.errorCode(), attempt.latencyMs(), attempt.startedAt());
    }
}
