package ukma.jpay.payments.domain;

import java.util.Set;

public enum TransactionStatus {
    INITIATED,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    REFUNDED;

    public boolean canTransitionTo(TransactionStatus target) {
        Set<TransactionStatus> allowed = switch (this) {
            case INITIATED -> Set.of(PROCESSING);
            case PROCESSING -> Set.of(SUCCEEDED, FAILED);
            case SUCCEEDED -> Set.of(REFUNDED);
            case FAILED, REFUNDED -> Set.of();
        };
        return allowed.contains(target);
    }
}
