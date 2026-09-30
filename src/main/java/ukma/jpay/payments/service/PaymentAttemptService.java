package ukma.jpay.payments.service;

import ukma.jpay.payments.domain.PaymentAttempt;
import ukma.jpay.payments.persistence.AttemptResult;

import java.util.List;
import java.util.UUID;

public interface PaymentAttemptService {

    PaymentAttempt create(UUID paymentId, String providerId, AttemptResult result);

    List<PaymentAttempt> list(UUID paymentId);

    PaymentAttempt get(UUID paymentId, UUID attemptId);

    PaymentAttempt updateResult(UUID paymentId, UUID attemptId, AttemptResult result);

    void delete(UUID paymentId, UUID attemptId);
}
