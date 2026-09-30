package ukma.jpay.payments.service;

import ukma.jpay.payments.domain.PaymentProvider;

import java.util.List;

public interface ProviderService {

    PaymentProvider create(String id, String name, int priority);

    List<PaymentProvider> list();

    PaymentProvider get(String providerId);

    PaymentProvider update(String providerId, String name, int priority);

    void delete(String providerId);
}
