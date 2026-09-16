package ukma.jpay.routing.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.util.Map;

public class ProviderNotFoundException extends BusinessException {

    private final String providerId;

    public ProviderNotFoundException(String providerId) {
        super(ProblemType.PROVIDER_NOT_FOUND, "Provider not found: " + providerId);
        this.providerId = providerId;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("providerId", providerId);
    }
}
