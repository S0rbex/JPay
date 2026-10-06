package ukma.jpay.shared.autoconfigure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import ukma.jpay.shared.notification.NotificationPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SharedModuleAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SharedModuleAutoConfiguration.class));

    @Test
    void createsPublisherByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(NotificationPublisher.class);
            assertThat(context).hasSingleBean(SharedModuleProperties.class);
            SharedModuleProperties properties = context.getBean(SharedModuleProperties.class);
            assertThat(properties.enabled()).isTrue();
            assertThat(properties.notifications().enabled()).isTrue();
            assertThat(properties.notifications().source()).isEqualTo("jpay");
        });
    }

    @ParameterizedTest
    @CsvSource({"true,true,true", "true,false,false", "false,true,false", "false,false,false"})
    void respectsModuleAndNotificationFlags(boolean moduleEnabled, boolean notificationsEnabled, boolean expected) {
        contextRunner.withPropertyValues(
                        "jpay.shared.enabled=" + moduleEnabled,
                        "jpay.shared.notifications.enabled=" + notificationsEnabled)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    if (expected) {
                        assertThat(context).hasSingleBean(NotificationPublisher.class);
                    } else {
                        assertThat(context).doesNotHaveBean(NotificationPublisher.class);
                    }
                    if (!moduleEnabled) {
                        assertThat(context).doesNotHaveBean(SharedModuleProperties.class);
                    }
                });
    }

    @Test
    void usesCustomPublisherInsteadOfDefault() {
        NotificationPublisher customPublisher = mock(NotificationPublisher.class);
        contextRunner.withBean(NotificationPublisher.class, () -> customPublisher)
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificationPublisher.class);
                    assertThat(context.getBean(NotificationPublisher.class)).isSameAs(customPublisher);
                });
    }

    @Test
    void bindsNestedProperties() {
        contextRunner.withPropertyValues("jpay.shared.notifications.source=team-payments")
                .run(context -> assertThat(context.getBean(SharedModuleProperties.class)
                        .notifications().source()).isEqualTo("team-payments"));
    }

    @Test
    void rejectsBlankSource() {
        contextRunner.withPropertyValues("jpay.shared.notifications.source=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void devProfileEnablesNotifications() {
        contextRunner.withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificationPublisher.class);
                    assertThat(context.getBean(SharedModuleProperties.class).notifications().source())
                            .isEqualTo("jpay-dev");
                });
    }

    @Test
    void prodProfileDisablesNotifications() {
        contextRunner.withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(NotificationPublisher.class);
                    assertThat(context.getBean(SharedModuleProperties.class).notifications().source())
                            .isEqualTo("jpay-prod");
                });
    }

    @Test
    void explicitPropertyOverridesProdProfile() {
        contextRunner.withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=prod", "jpay.shared.notifications.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificationPublisher.class);
                    assertThat(context.getBean(SharedModuleProperties.class).notifications().source())
                            .isEqualTo("jpay-prod");
                });
    }
}
