package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.util.Map;
import java.util.UUID;

public class AttemptNotFoundException extends BusinessException {

    private final UUID paymentId;
    private final UUID attemptId;

    public AttemptNotFoundException(UUID paymentId, UUID attemptId) {
        super(ProblemType.ATTEMPT_NOT_FOUND,
                "Attempt %s not found for payment %s".formatted(attemptId, paymentId));
        this.paymentId = paymentId;
        this.attemptId = attemptId;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("paymentId", paymentId.toString(), "attemptId", attemptId.toString());
    }
}
