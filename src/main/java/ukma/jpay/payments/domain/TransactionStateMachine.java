package ukma.jpay.payments.domain;

import ukma.jpay.payments.error.InvalidStateTransitionException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TransactionStateMachine {

    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED = Map.of(
            TransactionStatus.INITIATED, Set.of(TransactionStatus.PROCESSING, TransactionStatus.SUCCEEDED, TransactionStatus.FAILED),
            TransactionStatus.PROCESSING, Set.of(TransactionStatus.SUCCEEDED, TransactionStatus.FAILED),
            TransactionStatus.SUCCEEDED, Set.of(TransactionStatus.REFUNDED),
            TransactionStatus.FAILED, Set.of(),
            TransactionStatus.REFUNDED, Set.of());

    private TransactionStateMachine() {
    }

    public static boolean canTransition(TransactionStatus from, TransactionStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static Set<TransactionStatus> allowedFrom(TransactionStatus from) {
        return ALLOWED.getOrDefault(from, Set.of());
    }

    public static void requireTransition(UUID paymentId, TransactionStatus from, TransactionStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(paymentId, from, to, allowedFrom(from));
        }
    }
}
