package ukma.jpay.payments.web.dto;

import ukma.jpay.payments.domain.PaymentProvider;

public record ProviderResponse(String id, String name, int priority) {

    public static ProviderResponse from(PaymentProvider provider) {
        return new ProviderResponse(provider.id(), provider.name(), provider.priority());
    }
}
