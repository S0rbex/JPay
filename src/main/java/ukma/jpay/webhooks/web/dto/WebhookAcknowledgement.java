package ukma.jpay.webhooks.web.dto;

import java.util.UUID;

public record WebhookAcknowledgement(
        String providerId,
        String eventId,
        UUID paymentId) {
}
