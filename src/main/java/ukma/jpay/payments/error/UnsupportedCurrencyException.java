package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.util.Map;

public class UnsupportedCurrencyException extends BusinessException {

    private final String currency;

    public UnsupportedCurrencyException(String currency) {
        super(ProblemType.UNSUPPORTED_CURRENCY, "No provider supports currency: " + currency);
        this.currency = currency;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("currency", currency);
    }
}
