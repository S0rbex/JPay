package ukma.jpay.e2e;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.persistence.AttemptResult;
import ukma.jpay.payments.web.dto.AttemptResponse;
import ukma.jpay.payments.web.dto.MerchantResponse;
import ukma.jpay.payments.web.dto.PaymentResponse;
import ukma.jpay.security.AuthResponse;
import ukma.jpay.support.IntegrationTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@AutoConfigureRestTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentApiE2EIT {

    @Autowired
    private RestTestClient client;
    @Autowired
    private JdbcClient jdbc;

    private static String adminToken;
    private static String userToken;
    private static UUID merchantId;
    private static UUID paymentId;
    private static UUID attemptId;

    @Test
    @Order(1)
    void protectedEndpointRejectsAnonymousAndBadCredentials() {
        client.get().uri("/api/v1/merchants").exchange().expectStatus().isUnauthorized();

        client.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "admin", "password", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Order(2)
    void loginReturnsTokenForAdminAndUser() {
        adminToken = login("admin", "admin123");
        userToken = login("user", "user123");

        assertThat(adminToken).isNotBlank().contains(".");
        assertThat(userToken).isNotBlank().isNotEqualTo(adminToken);
    }

    @Test
    @Order(3)
    void adminCreatesMerchant() {
        Map<String, Object> request = Map.of(
                "name", "E2E Shop",
                "contactEmail", "e2e-" + UUID.randomUUID() + "@example.com",
                "defaultCurrency", "USD",
                "providerIds", List.of("stripe", "liqpay"));

        MerchantResponse merchant = client.post().uri("/api/v1/merchants")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody(MerchantResponse.class)
                .returnResult().getResponseBody();

        assertThat(merchant).isNotNull();
        assertThat(merchant.providerIds()).containsExactly("liqpay", "stripe");
        merchantId = merchant.id();
    }

    @Test
    @Order(4)
    void roleRulesHoldOverHttp() {
        client.get().uri("/api/v1/merchants")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .exchange()
                .expectStatus().isForbidden();

        client.get().uri("/api/v1/merchants")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.id == '%s')].name".formatted(merchantId)).isEqualTo("E2E Shop");
    }

    @Test
    @Order(5)
    void userCreatesPaymentThatStartsProcessing() {
        PaymentResponse payment = client.post().uri("/api/v1/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("merchantId", merchantId, "amount", "149.90", "currency", "USD",
                        "merchantReference", "e2e-order"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PaymentResponse.class)
                .returnResult().getResponseBody();

        assertThat(payment).isNotNull();
        assertThat(payment.status()).isEqualTo(TransactionStatus.PROCESSING);
        assertThat(payment.providerId()).isEqualTo("stripe");
        assertThat(payment.amount()).isEqualByComparingTo(new BigDecimal("149.90"));
        paymentId = payment.id();
    }

    @Test
    @Order(6)
    void userAddsPaymentAttempt() {
        AttemptResponse attempt = client.post().uri("/api/v1/payments/{id}/attempts", paymentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("providerId", "stripe", "result", "SUBMITTED"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AttemptResponse.class)
                .returnResult().getResponseBody();

        assertThat(attempt).isNotNull();
        assertThat(attempt.attemptNumber()).isEqualTo(2);
        assertThat(attempt.result()).isEqualTo(AttemptResult.SUBMITTED);
        attemptId = attempt.id();

        client.get().uri("/api/v1/payments/{id}/attempts", paymentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    @Order(7)
    void providerWebhookMarksPaymentSucceeded() {
        client.post().uri("/api/v1/providers/stripe/webhook-events")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("eventId", "evt-e2e-1", "paymentId", paymentId, "type", "PAYMENT_SUCCEEDED"))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.paymentId").isEqualTo(paymentId.toString())
                .jsonPath("$.providerId").isEqualTo("stripe");
    }

    @Test
    @Order(8)
    void finalStatusIsVisibleOverApiAndInDatabase() {
        PaymentResponse payment = client.get().uri("/api/v1/payments/{id}", paymentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResponse.class)
                .returnResult().getResponseBody();

        assertThat(payment).isNotNull();
        assertThat(payment.status()).isEqualTo(TransactionStatus.SUCCEEDED);
        assertThat(payment.merchantReference()).isEqualTo("e2e-order");
        assertThat(jdbc.sql("select status from payments where id = ?").param(paymentId).query(String.class).single())
                .isEqualTo("SUCCEEDED");
    }

    @Test
    @Order(9)
    void succeededPaymentCannotBeFailedOrDeleted() {
        client.post().uri("/api/v1/providers/stripe/webhook-events")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("eventId", "evt-e2e-2", "paymentId", paymentId, "type", "PAYMENT_FAILED"))
                .exchange()
                .expectStatus().isEqualTo(409);

        client.delete().uri("/api/v1/payments/{id}", paymentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .exchange()
                .expectStatus().isEqualTo(409);

        assertThat(jdbc.sql("select status from payments where id = ?").param(paymentId).query(String.class).single())
                .isEqualTo("SUCCEEDED");
    }

    private String login(String username, String password) {
        AuthResponse response = client.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", username, "password", password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        return response.token();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
