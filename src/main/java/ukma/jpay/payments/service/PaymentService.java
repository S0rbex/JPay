package ukma.jpay.payments.service;

import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {

    Payment create(BigDecimal amount, String currency, String merchantReference);

    Payment get(UUID paymentId);

    void updateStatus(UUID paymentId, String providerId, TransactionStatus status);
}
