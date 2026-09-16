package ukma.jpay.payments.service;

import ukma.jpay.merchants.domain.MerchantId;
import ukma.jpay.payments.domain.Refund;
import ukma.jpay.payments.error.CurrencyMismatchException;
import ukma.jpay.payments.error.InvalidStateTransitionException;
import ukma.jpay.payments.error.RefundAmountExceededException;
import ukma.jpay.payments.error.RefundNotFoundException;

import java.util.List;
import java.util.UUID;

public interface RefundService {

    Refund refund(MerchantId merchantId, UUID paymentId, CreateRefundCommand command);

    Refund getRefund(MerchantId merchantId, UUID paymentId, UUID refundId);

    List<Refund> listRefunds(MerchantId merchantId, UUID paymentId);
}
