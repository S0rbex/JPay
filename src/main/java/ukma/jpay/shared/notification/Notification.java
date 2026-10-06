package ukma.jpay.shared.notification;

import java.time.Instant;
import java.util.UUID;

public record Notification(String source, String type, UUID resourceId, Instant occurredAt) {
}
