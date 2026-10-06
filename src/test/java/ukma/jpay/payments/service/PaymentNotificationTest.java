package ukma.jpay.payments.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.event.EventListener;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.error.PaymentNotFoundException;
import ukma.jpay.shared.autoconfigure.SharedModuleAutoConfiguration;
import ukma.jpay.shared.notification.Notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentNotificationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SharedModuleAutoConfiguration.class))
            .withBean(InMemoryPaymentService.class)
            .withBean(NotificationCollector.class)
            .withPropertyValues("jpay.shared.notifications.source=payment-test");

    @Test
    void publishesNotificationsForCreationAndStatusChange() {
        contextRunner.run(context -> {
            PaymentService service = context.getBean(PaymentService.class);
            NotificationCollector collector = context.getBean(NotificationCollector.class);
            Instant startedAt = Instant.now();

            Payment payment = service.create(new BigDecimal("19.99"), "USD", "order-1");
            service.updateStatus(payment.id(), TransactionStatus.SUCCEEDED);

            assertThat(collector.notifications).extracting(Notification::type)
                    .containsExactly("payment.created", "payment.status-changed");
            assertThat(collector.notifications).allSatisfy(notification -> {
                assertThat(notification.source()).isEqualTo("payment-test");
                assertThat(notification.resourceId()).isEqualTo(payment.id());
                assertThat(notification.occurredAt()).isBetween(startedAt, Instant.now());
            });
            assertThat(service.get(payment.id()).status()).isEqualTo(TransactionStatus.SUCCEEDED);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"jpay.shared.enabled=false", "jpay.shared.notifications.enabled=false"})
    void paymentsWorkWithoutNotifications(String disabledProperty) {
        contextRunner.withPropertyValues(disabledProperty).run(context -> {
            PaymentService service = context.getBean(PaymentService.class);
            Payment payment = service.create(new BigDecimal("19.99"), "USD", "order-1");
            service.updateStatus(payment.id(), TransactionStatus.SUCCEEDED);

            assertThat(service.get(payment.id()).status()).isEqualTo(TransactionStatus.SUCCEEDED);
            assertThat(context.getBean(NotificationCollector.class).notifications).isEmpty();
        });
    }

    @Test
    void missingPaymentDoesNotPublishNotification() {
        contextRunner.run(context -> {
            PaymentService service = context.getBean(PaymentService.class);

            assertThatThrownBy(() -> service.updateStatus(UUID.randomUUID(), TransactionStatus.SUCCEEDED))
                    .isInstanceOf(PaymentNotFoundException.class);
            assertThat(context.getBean(NotificationCollector.class).notifications).isEmpty();
        });
    }

    static class NotificationCollector {

        private final List<Notification> notifications = new ArrayList<>();

        @EventListener
        public void receive(Notification notification) {
            notifications.add(notification);
        }
    }
}
