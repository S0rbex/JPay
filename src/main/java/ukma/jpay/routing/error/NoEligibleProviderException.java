package ukma.jpay.routing.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;
import ukma.jpay.common.error.RetryAfterHint;

import java.util.List;
import java.util.Map;

public class NoEligibleProviderException extends BusinessException implements RetryAfterHint {

    private final String currency;
    private final List<String> evaluatedProviders;

    public NoEligibleProviderException(String currency, List<String> evaluatedProviders) {
        super(ProblemType.NO_ELIGIBLE_PROVIDER, "No payment provider is currently available for " + currency);
        this.currency = currency;
        this.evaluatedProviders = List.copyOf(evaluatedProviders);
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("currency", currency, "evaluatedProviders", evaluatedProviders);
    }

    @Override
    public long retryAfterSeconds() {
        return 30;
    }
}
