package ukma.jpay.payments.service;

import java.math.BigDecimal;

public record CreatePaymentCommand(
        BigDecimal amount,
        String currency,
        String merchantReference,
        String description,
        String customerEmail,
        String customerPhone,
        String customerCountryCode) {
}
