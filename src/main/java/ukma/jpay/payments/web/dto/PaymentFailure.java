package ukma.jpay.payments.web.dto;

import ukma.jpay.payments.domain.PaymentFailureDetail;

public record PaymentFailure(String code, String message, String providerErrorCode) {

    public static PaymentFailure from(PaymentFailureDetail detail) {
        if (detail == null) {
            return null;
        }
        return new PaymentFailure(detail.code(), detail.message(), detail.providerErrorCode());
    }
}
