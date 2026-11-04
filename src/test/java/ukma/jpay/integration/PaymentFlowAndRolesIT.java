package ukma.jpay.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import ukma.jpay.support.AuthTokens;
import ukma.jpay.support.IntegrationTest;
import ukma.jpay.support.JpayApi;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class PaymentFlowAndRolesIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcClient jdbc;
    private JpayApi api;
    private String admin;
    private String user;

    @BeforeEach
    void login() throws Exception {
        api = new JpayApi(mockMvc);
        admin = AuthTokens.bearer(mockMvc, "admin", "admin123");
        user = AuthTokens.bearer(mockMvc, "user", "user123");
    }

    @Test
    void paymentGoesFromRequestThroughAttemptAndWebhookToSucceededRow() throws Exception {
        UUID merchantId = api.createMerchant(admin, "stripe");
        assertThat(count("merchants where id = ?", merchantId)).isEqualTo(1);

        UUID paymentId = api.createPayment(user, merchantId, "99.90", "USD", "flow-1");
        assertThat(statusOf(paymentId)).isEqualTo("PROCESSING");
        assertThat(count("payment_attempts where payment_id = ?", paymentId)).isEqualTo(1);

        mockMvc.perform(post("/api/v1/payments/{id}/attempts", paymentId)
                        .header(HttpHeaders.AUTHORIZATION, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": "stripe", "result": "SUBMITTED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptNumber").value(2));
        assertThat(count("payment_attempts where payment_id = ?", paymentId)).isEqualTo(2);
        assertThat(statusOf(paymentId)).isEqualTo("PROCESSING");

        api.webhook(admin, "stripe", paymentId, "PAYMENT_SUCCEEDED").andExpect(status().isAccepted());
        assertThat(statusOf(paymentId)).isEqualTo("SUCCEEDED");

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId).header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void adminCanListMerchants() throws Exception {
        UUID merchantId = api.createMerchant(admin, "stripe");

        mockMvc.perform(get("/api/v1/merchants").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].providerIds[0]".formatted(merchantId)).value("stripe"));
    }

    @Test
    void userGets403OnMerchantListAndNothingChanges() throws Exception {
        Map<String, Long> before = rowCounts();

        mockMvc.perform(get("/api/v1/merchants").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403));

        assertThat(rowCounts()).isEqualTo(before);
    }

    @Test
    void requestWithoutTokenGets401AndNothingChanges() throws Exception {
        UUID merchantId = api.createMerchant(admin, "stripe");
        UUID paymentId = api.createPayment(admin, merchantId, "5.00", "USD", "anon");
        Map<String, Long> before = rowCounts();

        mockMvc.perform(get("/api/v1/merchants"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"merchantId": "%s", "amount": 1.00, "currency": "USD"}
                                """.formatted(merchantId)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/payments/{id}/attempts", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": "stripe", "result": "FAILED"}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/providers/stripe/webhook-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": "anon", "paymentId": "%s", "type": "PAYMENT_FAILED"}
                                """.formatted(paymentId)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/payments/{id}", paymentId)).andExpect(status().isUnauthorized());

        assertThat(rowCounts()).isEqualTo(before);
        assertThat(statusOf(paymentId)).isEqualTo("PROCESSING");
    }

    @Test
    void garbageAndTamperedTokensAreTreatedAsAnonymous() throws Exception {
        Map<String, Long> before = rowCounts();
        String valid = admin;

        mockMvc.perform(get("/api/v1/merchants").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/merchants").header(HttpHeaders.AUTHORIZATION, valid + "x"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "admin", "password": "wrong"}
                                """))
                .andExpect(status().isUnauthorized());

        assertThat(rowCounts()).isEqualTo(before);
    }

    private Map<String, Long> rowCounts() {
        return Map.of(
                "merchants", count("merchants"),
                "payments", count("payments"),
                "payment_attempts", count("payment_attempts"),
                "users", count("users"));
    }

    private long count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }

    private long count(String fromAndWhere, UUID id) {
        return jdbc.sql("select count(*) from " + fromAndWhere).param(id).query(Long.class).single();
    }

    private String statusOf(UUID paymentId) {
        return jdbc.sql("select status from payments where id = ?").param(paymentId).query(String.class).single();
    }
}
