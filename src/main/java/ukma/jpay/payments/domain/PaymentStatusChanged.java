package ukma.jpay.payments.domain;

import java.util.UUID;

public record PaymentStatusChanged(UUID paymentId, TransactionStatus from, TransactionStatus to) {
}
