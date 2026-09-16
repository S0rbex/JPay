package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.util.Map;
import java.util.UUID;

public class CurrencyMismatchException extends BusinessException {

    private final UUID paymentId;
    private final String paymentCurrency;
    private final String requestedCurrency;

    public CurrencyMismatchException(UUID paymentId, String paymentCurrency, String requestedCurrency) {
        super(ProblemType.CURRENCY_MISMATCH, "Refund currency differs from the payment currency");
        this.paymentId = paymentId;
        this.paymentCurrency = paymentCurrency;
        this.requestedCurrency = requestedCurrency;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
                "paymentId", paymentId.toString(),
                "paymentCurrency", paymentCurrency,
                "requestedCurrency", requestedCurrency);
    }
}
