package ukma.jpay.payments.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionStatusTest {

    @ParameterizedTest
    @EnumSource(TransactionStatus.class)
    void onlyDocumentedTransitionsAreAllowed(TransactionStatus from) {
        var allowed = switch (from) {
            case INITIATED -> java.util.Set.of(TransactionStatus.PROCESSING);
            case PROCESSING -> java.util.Set.of(TransactionStatus.SUCCEEDED, TransactionStatus.FAILED);
            case SUCCEEDED -> java.util.Set.of(TransactionStatus.REFUNDED);
            case FAILED, REFUNDED -> java.util.Set.<TransactionStatus>of();
        };

        for (TransactionStatus to : TransactionStatus.values()) {
            assertThat(from.canTransitionTo(to)).isEqualTo(allowed.contains(to));
        }
    }
}
