package ukma.jpay.payments.provider;

import ukma.jpay.payments.domain.Payment;

public interface PaymentProviderClient {

    String providerId();

    boolean supports(String currency);

    void submit(Payment payment);
}
