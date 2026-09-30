package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.util.Map;
import java.util.UUID;

public class MerchantNotFoundException extends BusinessException {

    private final UUID merchantId;

    public MerchantNotFoundException(UUID merchantId) {
        super(ProblemType.MERCHANT_NOT_FOUND, "Merchant not found: " + merchantId);
        this.merchantId = merchantId;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("merchantId", merchantId.toString());
    }
}
