package ukma.jpay.payments.service;

import ukma.jpay.merchants.domain.MerchantId;
import ukma.jpay.payments.domain.Attempt;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.error.PaymentNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentService {

    Payment create(MerchantId merchantId, CreatePaymentCommand command);

    Payment get(MerchantId merchantId, UUID paymentId);

    Optional<Payment> findById(UUID paymentId);

    List<Attempt> listAttempts(MerchantId merchantId, UUID paymentId);

    Payment markRefunded(UUID paymentId, BigDecimal newRefundedAmount, TransactionStatus newStatus);
}
