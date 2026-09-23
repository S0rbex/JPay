package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;
import ukma.jpay.payments.domain.TransactionStatus;

import java.util.Map;
import java.util.UUID;

public class InvalidStateTransitionException extends BusinessException {

    private final UUID paymentId;
    private final TransactionStatus from;
    private final TransactionStatus to;

    public InvalidStateTransitionException(UUID paymentId, TransactionStatus from, TransactionStatus to) {
        super(ProblemType.INVALID_STATE_TRANSITION,
                "Cannot transition payment %s from %s to %s".formatted(paymentId, from, to));
        this.paymentId = paymentId;
        this.from = from;
        this.to = to;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
                "paymentId", paymentId.toString(),
                "from", from.name(),
                "to", to.name());
    }
}
