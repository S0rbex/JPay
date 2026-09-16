package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public class RefundAmountExceededException extends BusinessException {

    private final UUID paymentId;
    private final BigDecimal refundableAmount;
    private final BigDecimal requestedAmount;
    private final String currency;

    public RefundAmountExceededException(
            UUID paymentId, BigDecimal refundableAmount, BigDecimal requestedAmount, String currency) {
        super(ProblemType.REFUND_AMOUNT_EXCEEDED, "Refund amount exceeds the refundable amount");
        this.paymentId = paymentId;
        this.refundableAmount = refundableAmount;
        this.requestedAmount = requestedAmount;
        this.currency = currency;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
                "paymentId", paymentId.toString(),
                "refundableAmount", refundableAmount,
                "requestedAmount", requestedAmount,
                "currency", currency);
    }
}
