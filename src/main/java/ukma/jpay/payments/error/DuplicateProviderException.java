package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.util.Map;

public class DuplicateProviderException extends BusinessException {

    private final String providerId;

    public DuplicateProviderException(String providerId) {
        super(ProblemType.DUPLICATE_PROVIDER, "Provider already exists: " + providerId);
        this.providerId = providerId;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("providerId", providerId);
    }
}
