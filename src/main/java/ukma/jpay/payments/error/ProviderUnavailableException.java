package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

public class ProviderUnavailableException extends BusinessException {

    public ProviderUnavailableException(String providerId, String reason) {
        super(ProblemType.PROVIDER_UNAVAILABLE, "Provider %s is unavailable: %s".formatted(providerId, reason));
    }
}
