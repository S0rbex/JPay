package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.util.Map;
import java.util.UUID;

public class RefundNotFoundException extends BusinessException {

    private final UUID paymentId;
    private final UUID refundId;

    public RefundNotFoundException(UUID paymentId, UUID refundId) {
        super(ProblemType.REFUND_NOT_FOUND, "Refund not found: " + refundId);
        this.paymentId = paymentId;
        this.refundId = refundId;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("paymentId", paymentId.toString(), "refundId", refundId.toString());
    }
}
