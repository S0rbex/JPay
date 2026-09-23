package ukma.jpay.payments.repository;

import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID paymentId);

    boolean updateStatus(UUID paymentId, TransactionStatus from, TransactionStatus to);
}
