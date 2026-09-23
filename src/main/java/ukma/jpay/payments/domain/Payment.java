package ukma.jpay.payments.domain;

import ukma.jpay.payments.error.InvalidStateTransitionException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Payment(
        UUID id,
        TransactionStatus status,
        BigDecimal amount,
        String currency,
        String merchantReference,
        String providerId,
        Instant createdAt) {

    public Payment transitionTo(TransactionStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(id, status, target);
        }
        return new Payment(id, target, amount, currency, merchantReference, providerId, createdAt);
    }
}
