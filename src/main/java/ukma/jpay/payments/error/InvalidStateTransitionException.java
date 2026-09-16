package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;
import ukma.jpay.payments.domain.TransactionStatus;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InvalidStateTransitionException extends BusinessException {

    private final UUID paymentId;
    private final TransactionStatus current;
    private final TransactionStatus requested;
    private final Set<TransactionStatus> allowed;

    public InvalidStateTransitionException(
            UUID paymentId, TransactionStatus current, TransactionStatus requested, Set<TransactionStatus> allowed) {
        super(ProblemType.INVALID_STATE_TRANSITION,
                "Cannot transition payment from " + current + " to " + requested);
        this.paymentId = paymentId;
        this.current = current;
        this.requested = requested;
        this.allowed = Set.copyOf(allowed);
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
                "paymentId", paymentId.toString(),
                "currentStatus", current.name(),
                "requestedStatus", requested.name(),
                "allowedTransitions", allowed.stream().map(Enum::name).sorted().toList());
    }
}
