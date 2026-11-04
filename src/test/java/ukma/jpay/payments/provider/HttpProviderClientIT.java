package ukma.jpay.payments.provider;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.error.ProviderRejectedException;
import ukma.jpay.payments.error.ProviderTimeoutException;
import ukma.jpay.payments.error.ProviderUnavailableException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpProviderClientIT {

    private static final Duration READ_TIMEOUT = Duration.ofMillis(300);

    @RegisterExtension
    static WireMockExtension provider = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    private final UUID paymentId = UUID.randomUUID();
    private final Payment payment = new Payment(paymentId, TransactionStatus.INITIATED,
            new BigDecimal("12.34"), "USD", "ref-1", "stripe", Instant.now());

    @Test
    void successfulResponseSendsPaymentAsJson() {
        provider.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(200)));

        assertThatCode(() -> client(provider.baseUrl()).submit(payment)).doesNotThrowAnyException();

        provider.verify(1, postRequestedFor(urlEqualTo("/v1/payments"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("""
                        {"paymentId": "%s", "amount": 12.34, "currency": "USD", "merchantReference": "ref-1"}
                        """.formatted(paymentId))));
    }

    @Test
    void createdResponseCountsAsSuccess() {
        provider.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(201)));

        assertThatCode(() -> client(provider.baseUrl()).submit(payment)).doesNotThrowAnyException();
    }

    @Test
    void serverErrorBecomesProviderUnavailable() {
        provider.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client(provider.baseUrl()).submit(payment))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessageContaining("stripe")
                .hasMessageContaining("500");
    }

    @Test
    void badGatewayAlsoBecomesProviderUnavailable() {
        provider.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client(provider.baseUrl()).submit(payment))
                .isInstanceOf(ProviderUnavailableException.class);
    }

    @Test
    void clientErrorBecomesProviderRejected() {
        provider.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(402)));

        assertThatThrownBy(() -> client(provider.baseUrl()).submit(payment))
                .isInstanceOf(ProviderRejectedException.class)
                .hasMessageContaining("402");
    }

    @Test
    void slowResponseBecomesProviderTimeoutWithinTheConfiguredReadTimeout() {
        provider.stubFor(post(urlEqualTo("/v1/payments"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(2000)));

        long started = System.nanoTime();
        assertThatThrownBy(() -> client(provider.baseUrl()).submit(payment))
                .isInstanceOf(ProviderTimeoutException.class);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        assertThat(elapsed).isLessThan(Duration.ofMillis(1800));
    }

    @Test
    void connectionResetBecomesProviderUnavailable() {
        provider.stubFor(post(urlEqualTo("/v1/payments"))
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> client(provider.baseUrl()).submit(payment))
                .isInstanceOf(ProviderUnavailableException.class);
    }

    @Test
    void unreachableHostBecomesProviderUnavailable() throws IOException {
        assertThatThrownBy(() -> client("http://localhost:" + freePort()).submit(payment))
                .isInstanceOf(ProviderUnavailableException.class);
    }

    @Test
    void supportsOnlyItsOwnCurrencies() {
        HttpProviderClient client = client(provider.baseUrl());

        assertThat(client.providerId()).isEqualTo("stripe");
        assertThat(client.supports("USD")).isTrue();
        assertThat(client.supports("EUR")).isTrue();
        assertThat(client.supports("UAH")).isFalse();
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static HttpProviderClient client(String baseUrl) {
        var endpoint = new ProviderHttpProperties.Endpoint("http", baseUrl, Duration.ofSeconds(1), READ_TIMEOUT);
        return new StripeHttpProviderClient(ProviderHttpConfig.restClient(endpoint));
    }
}
