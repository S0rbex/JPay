package ukma.jpay.payments.web.dto;

import ukma.jpay.payments.domain.Refund;
import ukma.jpay.payments.domain.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID paymentId,
        BigDecimal amount,
        String currency,
        RefundStatus status,
        String reason,
        Instant createdAt) {

    public static RefundResponse from(Refund refund) {
        return new RefundResponse(refund.id(), refund.paymentId(), refund.amount(), refund.currency(),
                refund.status(), refund.reason(), refund.createdAt());
    }
}
