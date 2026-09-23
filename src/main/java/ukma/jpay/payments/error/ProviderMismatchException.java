package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.util.Map;
import java.util.UUID;

public class ProviderMismatchException extends BusinessException {

    private final UUID paymentId;
    private final String expectedProviderId;
    private final String actualProviderId;

    public ProviderMismatchException(UUID paymentId, String expectedProviderId, String actualProviderId) {
        super(ProblemType.PROVIDER_MISMATCH,
                "Payment %s belongs to provider %s, not %s"
                        .formatted(paymentId, expectedProviderId, actualProviderId));
        this.paymentId = paymentId;
        this.expectedProviderId = expectedProviderId;
        this.actualProviderId = actualProviderId;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of(
                "paymentId", paymentId.toString(),
                "expectedProviderId", expectedProviderId,
                "actualProviderId", actualProviderId);
    }
}
