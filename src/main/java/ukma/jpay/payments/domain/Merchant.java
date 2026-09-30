package ukma.jpay.payments.domain;

import java.util.List;
import java.util.UUID;

public record Merchant(
        UUID id,
        String name,
        String contactEmail,
        String defaultCurrency,
        List<String> providerIds) {

    public Merchant {
        providerIds = List.copyOf(providerIds);
    }
}
