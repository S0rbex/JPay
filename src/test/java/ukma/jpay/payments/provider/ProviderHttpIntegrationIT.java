package ukma.jpay.payments.provider;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ukma.jpay.support.AuthTokens;
import ukma.jpay.support.IntegrationTest;
import ukma.jpay.support.JpayApi;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProviderHttpIntegrationIT {

    @RegisterExtension
    static WireMockExtension stripe = WireMockExtension.newInstance()
            .options(options().dynamicPort())
            .build();

    @DynamicPropertySource
    static void providerProperties(DynamicPropertyRegistry registry) {
        registry.add("jpay.providers.stripe.mode", () -> "http");
        registry.add("jpay.providers.stripe.base-url", stripe::baseUrl);
        registry.add("jpay.providers.stripe.read-timeout", () -> "500ms");
    }

    private JpayApi api;
    @Autowired
    private MockMvc mockMvc;
    private String user;
    private String admin;
    @Autowired
    private JdbcClient jdbc;
    private UUID merchantId;

    @BeforeEach
    void setUp() throws Exception {
        api = new JpayApi(mockMvc);
        admin = AuthTokens.bearer(mockMvc, "admin", "admin123");
        user = AuthTokens.bearer(mockMvc, "user", "user123");
        merchantId = api.createMerchant(admin, "stripe");
        stripe.resetAll();
    }

    @Test
    void providerAcceptsPaymentAndItIsStoredAsProcessing() throws Exception {
        stripe.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(200)));

        UUID paymentId = api.createPayment(user, merchantId, "7.00", "USD", "http-ok");

        stripe.verify(1, postRequestedFor(urlEqualTo("/v1/payments")));
        assertThat(jdbc.sql("select status from payments where id = ?").param(paymentId).query(String.class).single())
                .isEqualTo("PROCESSING");
    }

    @Test
    void provider500Gives502AndRollsBackThePayment() throws Exception {
        stripe.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(500)));
        long before = payments("http-500");

        api.postPayment(user, merchantId, "7.00", "USD", "http-500")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Payment provider unavailable"));

        assertThat(payments("http-500")).isEqualTo(before).isZero();
        assertThat(jdbc.sql("select count(*) from payment_attempts a join payments p on a.payment_id = p.id "
                + "where p.merchant_reference = 'http-500'").query(Long.class).single()).isZero();
    }

    @Test
    void providerTimeoutGives504AndRollsBackThePayment() throws Exception {
        stripe.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(200).withFixedDelay(3000)));

        api.postPayment(user, merchantId, "7.00", "USD", "http-timeout")
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.title").value("Payment provider timed out"));

        assertThat(payments("http-timeout")).isZero();
    }

    @Test
    void providerRejectionGives422AndRollsBackThePayment() throws Exception {
        stripe.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(400)));

        api.postPayment(user, merchantId, "7.00", "USD", "http-rejected")
                .andExpect(status().isUnprocessableContent());

        assertThat(payments("http-rejected")).isZero();
    }

    private long payments(String reference) {
        return jdbc.sql("select count(*) from payments where merchant_reference = ?")
                .param(reference).query(Long.class).single();
    }
}
