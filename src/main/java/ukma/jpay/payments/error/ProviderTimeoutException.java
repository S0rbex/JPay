package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

public class ProviderTimeoutException extends BusinessException {

    public ProviderTimeoutException(String providerId) {
        super(ProblemType.PROVIDER_TIMEOUT, "Provider %s did not respond in time".formatted(providerId));
    }
}
