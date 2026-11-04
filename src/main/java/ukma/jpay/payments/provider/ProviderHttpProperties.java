package ukma.jpay.payments.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties("jpay")
record ProviderHttpProperties(Map<String, Endpoint> providers) {

    ProviderHttpProperties {
        providers = providers == null ? Map.of() : providers;
    }

    Endpoint endpoint(String providerId) {
        Endpoint endpoint = providers.get(providerId);
        if (endpoint == null || endpoint.baseUrl() == null) {
            throw new IllegalStateException("jpay.providers.%s.base-url must be set when mode=http".formatted(providerId));
        }
        return endpoint;
    }

    record Endpoint(String mode, String baseUrl, Duration connectTimeout, Duration readTimeout) {

        Endpoint {
            connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
            readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
        }
    }
}
