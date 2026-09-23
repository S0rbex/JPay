package ukma.jpay.webhooks.domain;

import ukma.jpay.payments.domain.TransactionStatus;

public enum ProviderEventType {
    PAYMENT_SUCCEEDED(TransactionStatus.SUCCEEDED),
    PAYMENT_FAILED(TransactionStatus.FAILED),
    PAYMENT_REFUNDED(TransactionStatus.REFUNDED);

    private final TransactionStatus targetStatus;

    ProviderEventType(TransactionStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public TransactionStatus targetStatus() {
        return targetStatus;
    }
}
