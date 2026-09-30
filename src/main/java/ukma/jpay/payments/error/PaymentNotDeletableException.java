package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;
import ukma.jpay.payments.domain.TransactionStatus;

import java.util.Map;
import java.util.UUID;

public class PaymentNotDeletableException extends BusinessException {

    private final UUID paymentId;
    private final TransactionStatus status;

    public PaymentNotDeletableException(UUID paymentId, TransactionStatus status) {
        super(ProblemType.PAYMENT_NOT_DELETABLE,
                "Payment %s in status %s cannot be deleted".formatted(paymentId, status));
        this.paymentId = paymentId;
        this.status = status;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("paymentId", paymentId.toString(), "currentStatus", status.name());
    }
}
