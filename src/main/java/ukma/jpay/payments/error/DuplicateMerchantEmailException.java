package ukma.jpay.payments.error;

import ukma.jpay.common.error.BusinessException;
import ukma.jpay.common.error.ProblemType;

import java.util.Map;

public class DuplicateMerchantEmailException extends BusinessException {

    private final String contactEmail;

    public DuplicateMerchantEmailException(String contactEmail) {
        super(ProblemType.DUPLICATE_MERCHANT_EMAIL, "Merchant email already in use: " + contactEmail);
        this.contactEmail = contactEmail;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("contactEmail", contactEmail);
    }
}
