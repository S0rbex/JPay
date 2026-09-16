package ukma.jpay.payments.service;

import org.springframework.stereotype.Service;
import ukma.jpay.merchants.domain.MerchantId;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.Refund;
import ukma.jpay.payments.domain.RefundStatus;
import ukma.jpay.payments.domain.TransactionStateMachine;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.error.CurrencyMismatchException;
import ukma.jpay.payments.error.RefundAmountExceededException;
import ukma.jpay.payments.error.RefundNotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class InMemoryRefundService implements RefundService {

    private final PaymentService paymentService;
    private final Map<UUID, List<Refund>> refundsByPayment = new ConcurrentHashMap<>();

    public InMemoryRefundService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Refund refund(MerchantId merchantId, UUID paymentId, CreateRefundCommand command) {
        Payment payment = paymentService.get(merchantId, paymentId);

        TransactionStateMachine.requireTransition(paymentId, payment.status(), TransactionStatus.REFUNDED);

        if (!payment.currency().equals(command.currency())) {
            throw new CurrencyMismatchException(paymentId, payment.currency(), command.currency());
        }

        BigDecimal refundable = payment.amount().subtract(payment.refundedAmount());
        if (command.amount().compareTo(refundable) > 0) {
            throw new RefundAmountExceededException(paymentId, refundable, command.amount(), payment.currency());
        }

        paymentService.markRefunded(paymentId, payment.refundedAmount().add(command.amount()), TransactionStatus.REFUNDED);

        Refund refund = new Refund(UUID.randomUUID(), paymentId, command.amount(), command.currency(),
                RefundStatus.SUCCEEDED, command.reason(), Instant.now());
        refundsByPayment.computeIfAbsent(paymentId, id -> new CopyOnWriteArrayList<>()).add(refund);
        return refund;
    }

    @Override
    public Refund getRefund(MerchantId merchantId, UUID paymentId, UUID refundId) {
        paymentService.get(merchantId, paymentId);
        return refundsByPayment.getOrDefault(paymentId, List.of()).stream()
                .filter(refund -> refund.id().equals(refundId))
                .findFirst()
                .orElseThrow(() -> new RefundNotFoundException(paymentId, refundId));
    }

    @Override
    public List<Refund> listRefunds(MerchantId merchantId, UUID paymentId) {
        paymentService.get(merchantId, paymentId);
        return List.copyOf(refundsByPayment.getOrDefault(paymentId, List.of()));
    }
}
