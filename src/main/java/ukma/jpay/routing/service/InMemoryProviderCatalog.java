package ukma.jpay.routing.service;

import org.springframework.stereotype.Service;
import ukma.jpay.routing.domain.PaymentProvider;
import ukma.jpay.routing.domain.ProviderHealth;
import ukma.jpay.routing.error.NoEligibleProviderException;
import ukma.jpay.routing.error.ProviderNotFoundException;
import ukma.jpay.routing.error.UnsupportedCurrencyException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class InMemoryProviderCatalog implements ProviderRouter {

    private final Map<String, PaymentProvider> byId = new ConcurrentHashMap<>();

    public InMemoryProviderCatalog() {
        register(new PaymentProvider("stripe", "Stripe",
                Set.of("USD", "EUR"), new BigDecimal("2.9"), 1, ProviderHealth.HEALTHY));
        register(new PaymentProvider("adyen", "Adyen",
                Set.of("USD", "EUR", "GBP"), new BigDecimal("2.5"), 2, ProviderHealth.HEALTHY));
        register(new PaymentProvider("liqpay", "LiqPay",
                Set.of("UAH"), new BigDecimal("2.0"), 1, ProviderHealth.HEALTHY));
    }

    private void register(PaymentProvider provider) {
        byId.put(provider.id(), provider);
    }

    @Override
    public PaymentProvider selectProvider(String currency) {
        List<PaymentProvider> eligible = healthyProvidersFor(currency);
        if (!eligible.isEmpty()) {
            return eligible.getFirst();
        }
        boolean currencySupportedByAnyProvider = byId.values().stream()
                .anyMatch(provider -> provider.supportedCurrencies().contains(currency));
        if (!currencySupportedByAnyProvider) {
            throw new UnsupportedCurrencyException(currency, allSupportedCurrencies());
        }
        throw new NoEligibleProviderException(currency, byId.keySet().stream().sorted().toList());
    }

    @Override
    public PaymentProvider getProvider(String providerId) {
        PaymentProvider provider = byId.get(providerId);
        if (provider == null) {
            throw new ProviderNotFoundException(providerId);
        }
        return provider;
    }

    @Override
    public List<PaymentProvider> listProviders(String currencyFilter) {
        return byId.values().stream()
                .filter(provider -> currencyFilter == null || provider.supportedCurrencies().contains(currencyFilter))
                .sorted(Comparator.comparing(PaymentProvider::priority).thenComparing(PaymentProvider::id))
                .toList();
    }

    private List<PaymentProvider> healthyProvidersFor(String currency) {
        return byId.values().stream()
                .filter(provider -> provider.supportedCurrencies().contains(currency))
                .filter(provider -> provider.health() == ProviderHealth.HEALTHY)
                .sorted(Comparator.comparing(PaymentProvider::priority))
                .toList();
    }

    private Set<String> allSupportedCurrencies() {
        return byId.values().stream()
                .flatMap(provider -> provider.supportedCurrencies().stream())
                .collect(Collectors.toCollection(java.util.TreeSet::new));
    }
}
