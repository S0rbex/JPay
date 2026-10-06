package ukma.jpay.webhooks.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ukma.jpay.webhooks.domain.ProviderEventType;

import java.util.UUID;

public record ProviderWebhookRequest(
        @Schema(example = "evt-1")
        @NotBlank @Size(max = 128) String eventId,
        @Schema(example = "83ec3956-d8e0-4c88-8147-70787150617a")
        @NotNull UUID paymentId,
        @Schema(example = "PAYMENT_SUCCEEDED")
        @NotNull ProviderEventType type) {
}
