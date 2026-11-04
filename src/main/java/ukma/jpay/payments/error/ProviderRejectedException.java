package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

public class ProviderRejectedException extends BusinessException {

    public ProviderRejectedException(String providerId, int httpStatus) {
        super(ProblemType.PROVIDER_REJECTED,
                "Provider %s rejected the payment with HTTP %d".formatted(providerId, httpStatus));
    }
}
