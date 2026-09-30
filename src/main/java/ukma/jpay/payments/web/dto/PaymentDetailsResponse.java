package ukma.jpay.payments.web.dto;

import ukma.jpay.payments.domain.PaymentDetails;
import ukma.jpay.payments.domain.PaymentDetails.AttemptSummary;
import ukma.jpay.payments.domain.PaymentDetails.MerchantSummary;
import ukma.jpay.payments.domain.PaymentDetails.ProviderSummary;
import ukma.jpay.payments.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentDetailsResponse(
        UUID id,
        TransactionStatus status,
        BigDecimal amount,
        String currency,
        String merchantReference,
        String providerId,
        Instant createdAt,
        MerchantSummary merchant,
        ProviderSummary provider,
        List<AttemptSummary> attempts) {

    public static PaymentDetailsResponse from(PaymentDetails details) {
        var payment = details.payment();
        return new PaymentDetailsResponse(payment.id(), payment.status(), payment.amount(),
                payment.currency(), payment.merchantReference(), payment.providerId(), payment.createdAt(),
                details.merchant(), details.provider(), details.attempts());
    }
}
