package ukma.jpay.payments.repository;

import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.PaymentDetails;
import ukma.jpay.payments.domain.TransactionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment create(Payment payment, UUID merchantId);

    Payment save(Payment payment);

    void deleteById(UUID paymentId);

    Optional<Payment> findById(UUID paymentId);

    boolean updateStatus(UUID paymentId, TransactionStatus from, TransactionStatus to);

    List<PaymentDetails> findAllWithDetails();
}
