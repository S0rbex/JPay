package ukma.jpay.routing.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.util.Map;
import java.util.Set;

public class UnsupportedCurrencyException extends BusinessException {

    private final String currency;
    private final Set<String> supportedCurrencies;

    public UnsupportedCurrencyException(String currency, Set<String> supportedCurrencies) {
        super(ProblemType.UNSUPPORTED_CURRENCY, "Currency is not supported by any provider: " + currency);
        this.currency = currency;
        this.supportedCurrencies = Set.copyOf(supportedCurrencies);
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("currency", currency, "supportedCurrencies", supportedCurrencies);
    }
}
