package ukma.jpay.routing.domain;

import java.math.BigDecimal;
import java.util.Set;

public record PaymentProvider(
        String id,
        String name,
        Set<String> supportedCurrencies,
        BigDecimal feePercent,
        int priority,
        ProviderHealth health) {

    public PaymentProvider {
        supportedCurrencies = Set.copyOf(supportedCurrencies);
    }
}
