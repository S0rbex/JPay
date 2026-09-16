package ukma.jpay.payments.domain;

import ukma.jpay.merchants.domain.MerchantId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Payment(
        UUID id,
        MerchantId merchantId,
        TransactionStatus status,
        BigDecimal amount,
        String currency,
        String providerId,
        String providerName,
        String merchantReference,
        String description,
        PaymentFailureDetail failure,
        BigDecimal refundedAmount,
        Instant createdAt,
        Instant updatedAt) {
}
