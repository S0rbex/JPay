package ukma.jpay.shared.notification;

import java.util.UUID;

@FunctionalInterface
public interface NotificationPublisher {

    void publish(String type, UUID resourceId);
}
