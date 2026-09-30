package ukma.jpay.payments.service;

import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.PaymentDetails;
import ukma.jpay.payments.domain.TransactionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentService {

    Payment create(UUID merchantId, BigDecimal amount, String currency, String merchantReference);

    Payment get(UUID paymentId);

    List<PaymentDetails> list();

    Payment updateReference(UUID paymentId, String merchantReference);

    void delete(UUID paymentId);

    void updateStatus(UUID paymentId, String providerId, TransactionStatus status);
}
