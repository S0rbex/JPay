package ukma.jpay.webhooks.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ukma.jpay.webhooks.domain.ProviderEventType;

import java.util.UUID;

public record ProviderWebhookRequest(
        @NotBlank @Size(max = 128) String eventId,
        @NotNull UUID paymentId,
        @NotNull ProviderEventType type) {
}
