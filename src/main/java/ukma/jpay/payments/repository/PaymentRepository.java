package ukma.jpay.payments.repository;

import ukma.jpay.payments.domain.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID paymentId);
}
