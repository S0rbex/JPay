package ukma.jpay.payments.domain;

public record PaymentFailureDetail(String code, String message, String providerErrorCode) {
}
