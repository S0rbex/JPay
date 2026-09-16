package ukma.jpay.routing.web.dto;

import ukma.jpay.routing.domain.PaymentProvider;
import ukma.jpay.routing.domain.ProviderHealth;

import java.math.BigDecimal;
import java.util.Set;

public record ProviderResponse(
        String id,
        String name,
        Set<String> supportedCurrencies,
        BigDecimal feePercent,
        int priority,
        ProviderHealth health) {

    public ProviderResponse {
        supportedCurrencies = Set.copyOf(supportedCurrencies);
    }

    public static ProviderResponse from(PaymentProvider provider) {
        return new ProviderResponse(provider.id(), provider.name(), provider.supportedCurrencies(),
                provider.feePercent(), provider.priority(), provider.health());
    }
}
