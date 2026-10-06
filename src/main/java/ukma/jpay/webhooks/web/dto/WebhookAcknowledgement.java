package ukma.jpay.webhooks.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record WebhookAcknowledgement(
        @Schema(example = "stripe")
        String providerId,
        @Schema(example = "evt-1")
        String eventId,
        @Schema(example = "83ec3956-d8e0-4c88-8147-70787150617a")
        UUID paymentId) {
}
