package ukma.jpay.routing.service;

import ukma.jpay.routing.domain.PaymentProvider;
import ukma.jpay.routing.error.NoEligibleProviderException;
import ukma.jpay.routing.error.ProviderNotFoundException;
import ukma.jpay.routing.error.UnsupportedCurrencyException;

import java.util.List;

public interface ProviderRouter {

    PaymentProvider selectProvider(String currency);

    PaymentProvider getProvider(String providerId);

    List<PaymentProvider> listProviders(String currencyFilter);
}
