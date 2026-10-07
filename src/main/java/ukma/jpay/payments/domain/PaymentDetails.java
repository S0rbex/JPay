package ukma.jpay.payments.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentDetails(
        Payment payment,
        MerchantSummary merchant,
        ProviderSummary provider,
        List<AttemptSummary> attempts) {

    public PaymentDetails {
        attempts = List.copyOf(attempts);
    }

    public record MerchantSummary(UUID id, String name) {
    }

    public record ProviderSummary(String id, String name) {
    }

    public record AttemptSummary(UUID id, int attemptNumber, String result,
                                 ProviderSummary provider, Instant createdAt) {
    }
}
