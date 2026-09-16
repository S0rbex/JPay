package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.util.Map;
import java.util.UUID;

public class PaymentNotFoundException extends BusinessException {

    private final UUID paymentId;

    public PaymentNotFoundException(UUID paymentId) {
        super(ProblemType.PAYMENT_NOT_FOUND, "Payment not found: " + paymentId);
        this.paymentId = paymentId;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("paymentId", paymentId.toString());
    }
}
