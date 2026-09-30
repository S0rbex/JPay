package ukma.jpay.payments.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ukma.jpay.payments.domain.PaymentAttempt;
import ukma.jpay.payments.error.AttemptNotFoundException;
import ukma.jpay.payments.error.PaymentNotFoundException;
import ukma.jpay.payments.error.ProviderNotFoundException;
import ukma.jpay.payments.persistence.AttemptResult;
import ukma.jpay.payments.persistence.PaymentAttemptEntity;
import ukma.jpay.payments.persistence.PaymentEntity;
import ukma.jpay.payments.repository.JpaPaymentRepository;
import ukma.jpay.payments.repository.PaymentAttemptRepository;
import ukma.jpay.payments.repository.PaymentProviderRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class PaymentAttemptServiceImpl implements PaymentAttemptService {

    private final JpaPaymentRepository payments;
    private final PaymentAttemptRepository attempts;
    private final PaymentProviderRepository providers;

    PaymentAttemptServiceImpl(JpaPaymentRepository payments, PaymentAttemptRepository attempts,
                              PaymentProviderRepository providers) {
        this.payments = payments;
        this.attempts = attempts;
        this.providers = providers;
    }

    @Override
    @Transactional
    public PaymentAttempt create(UUID paymentId, String providerId, AttemptResult result) {
        PaymentEntity payment = payments.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        var provider = providers.findById(providerId)
                .orElseThrow(() -> new ProviderNotFoundException(providerId));
        int number = attempts.findMaxAttemptNumber(paymentId) + 1;

        var attempt = new PaymentAttemptEntity(provider, number, result, Instant.now());
        payment.addAttempt(attempt);
        payments.flush();
        return toDomain(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAttempt> list(UUID paymentId) {
        requirePayment(paymentId);
        return attempts.findWithDetailsByPaymentId(paymentId).stream()
                .map(PaymentAttemptServiceImpl::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentAttempt get(UUID paymentId, UUID attemptId) {
        requirePayment(paymentId);
        return toDomain(find(paymentId, attemptId));
    }

    @Override
    @Transactional
    public PaymentAttempt updateResult(UUID paymentId, UUID attemptId, AttemptResult result) {
        requirePayment(paymentId);
        PaymentAttemptEntity attempt = find(paymentId, attemptId);
        attempt.setResult(result);
        return toDomain(attempt);
    }

    @Override
    @Transactional
    public void delete(UUID paymentId, UUID attemptId) {
        requirePayment(paymentId);
        PaymentAttemptEntity attempt = find(paymentId, attemptId);
        attempt.getPayment().removeAttempt(attempt);
    }

    private void requirePayment(UUID paymentId) {
        if (!payments.existsById(paymentId)) {
            throw new PaymentNotFoundException(paymentId);
        }
    }

    private PaymentAttemptEntity find(UUID paymentId, UUID attemptId) {
        return attempts.findByIdAndPaymentId(attemptId, paymentId)
                .orElseThrow(() -> new AttemptNotFoundException(paymentId, attemptId));
    }

    private static PaymentAttempt toDomain(PaymentAttemptEntity entity) {
        return new PaymentAttempt(entity.getId(), entity.getPayment().getId(),
                entity.getProvider().getId(), entity.getAttemptNumber(), entity.getResult(),
                entity.getCreatedAt());
    }
}
