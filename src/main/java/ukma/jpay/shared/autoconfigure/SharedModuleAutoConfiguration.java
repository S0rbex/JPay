package ukma.jpay.shared.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import ukma.jpay.shared.notification.Notification;
import ukma.jpay.shared.notification.NotificationPublisher;

import java.time.Instant;

@AutoConfiguration
@EnableConfigurationProperties(SharedModuleProperties.class)
@ConditionalOnProperty(prefix = "jpay.shared", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SharedModuleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(NotificationPublisher.class)
    @ConditionalOnProperty(prefix = "jpay.shared.notifications", name = "enabled", havingValue = "true",
            matchIfMissing = true)
    public NotificationPublisher notificationPublisher(
            ApplicationEventPublisher eventPublisher, SharedModuleProperties properties) {
        return (type, resourceId) -> eventPublisher.publishEvent(new Notification(
                properties.notifications().source(), type, resourceId, Instant.now()));
    }
}
