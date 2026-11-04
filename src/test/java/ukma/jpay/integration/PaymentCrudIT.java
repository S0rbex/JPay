package ukma.jpay.integration;

import com.jayway.jsonpath.JsonPath;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class PaymentCrudIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcClient jdbc;
    private JpayApi api;
    private String admin;
    private String user;

    private UUID merchantId;

    @BeforeEach
    void createMerchant() throws Exception {
        api = new JpayApi(mockMvc);
        admin = AuthTokens.bearer(mockMvc, "admin", "admin123");
        user = AuthTokens.bearer(mockMvc, "user", "user123");
        merchantId = api.createMerchant(admin, "stripe", "liqpay");
    }

    @Test
    void createPaymentInsertsPaymentRowAndFirstSubmittedAttempt() throws Exception {
        String response = api.postPayment(user, merchantId, "19.99", "USD", "order-create")
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.providerId").value("stripe"))
                .andReturn().getResponse().getContentAsString();
        UUID paymentId = UUID.fromString(JsonPath.read(response, "$.id"));

        Map<String, Object> row = payment(paymentId);
        assertThat(row).containsEntry("status", "PROCESSING")
                .containsEntry("currency", "USD")
                .containsEntry("merchant_reference", "order-create")
                .containsEntry("provider_id", "stripe")
                .containsEntry("merchant_id", merchantId);
        assertThat((BigDecimal) row.get("amount")).isEqualByComparingTo("19.99");

        List<Map<String, Object>> attempts = attempts(paymentId);
        assertThat(attempts).hasSize(1);
        assertThat(attempts.getFirst()).containsEntry("attempt_number", 1)
                .containsEntry("result", "SUBMITTED")
                .containsEntry("provider_id", "stripe");
    }

    @Test
    void readPaymentByIdAndInListMatchesDatabase() throws Exception {
        UUID paymentId = api.createPayment(admin, merchantId, "42.50", "EUR", "order-read");

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId).header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.amount").value(42.50))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.merchantReference").value("order-read"));

        mockMvc.perform(get("/api/v1/payments").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].attempts.length()".formatted(paymentId)).value(1))
                .andExpect(jsonPath("$[?(@.id == '%s')].merchant.id".formatted(paymentId)).value(merchantId.toString()));

        assertThat(payment(paymentId)).containsEntry("currency", "EUR");
        mockMvc.perform(get("/api/v1/payments/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMerchantReferenceChangesOnlyThatColumn() throws Exception {
        UUID paymentId = api.createPayment(admin, merchantId, "10.00", "USD", "before");

        mockMvc.perform(patch("/api/v1/payments/{id}", paymentId)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"merchantReference": "after"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantReference").value("after"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        assertThat(payment(paymentId)).containsEntry("merchant_reference", "after")
                .containsEntry("status", "PROCESSING");
    }

    @Test
    void webhookUpdatesPaymentStatusAndRejectsIllegalTransition() throws Exception {
        UUID paymentId = api.createPayment(admin, merchantId, "10.00", "USD", "order-status");

        api.webhook(admin, "stripe", paymentId, "PAYMENT_SUCCEEDED").andExpect(status().isAccepted());
        assertThat(payment(paymentId)).containsEntry("status", "SUCCEEDED");

        api.webhook(admin, "stripe", paymentId, "PAYMENT_FAILED").andExpect(status().isConflict());
        assertThat(payment(paymentId)).containsEntry("status", "SUCCEEDED");

        api.webhook(admin, "liqpay", paymentId, "PAYMENT_REFUNDED").andExpect(status().isConflict());
        assertThat(payment(paymentId)).containsEntry("status", "SUCCEEDED");

        api.webhook(admin, "stripe", paymentId, "PAYMENT_REFUNDED").andExpect(status().isAccepted());
        assertThat(payment(paymentId)).containsEntry("status", "REFUNDED");
    }

    @Test
    void deleteIsAllowedOnlyForFailedPaymentAndRemovesItsAttempts() throws Exception {
        UUID paymentId = api.createPayment(admin, merchantId, "10.00", "USD", "order-delete");

        mockMvc.perform(delete("/api/v1/payments/{id}", paymentId).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict());
        assertThat(count("payments", paymentId)).isEqualTo(1);
        assertThat(attempts(paymentId)).hasSize(1);

        api.webhook(admin, "stripe", paymentId, "PAYMENT_FAILED").andExpect(status().isAccepted());
        assertThat(payment(paymentId)).containsEntry("status", "FAILED");

        mockMvc.perform(delete("/api/v1/payments/{id}", paymentId).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        assertThat(count("payments", paymentId)).isZero();
        assertThat(attempts(paymentId)).isEmpty();
        mockMvc.perform(get("/api/v1/payments/{id}", paymentId).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidPaymentRequestsAreRejectedAndNothingIsInserted() throws Exception {
        long before = jdbc.sql("select count(*) from payments").query(Long.class).single();

        api.postPayment(admin, merchantId, "0", "USD", "bad-amount").andExpect(status().isBadRequest());
        api.postPayment(admin, merchantId, "5.00", "usd", "bad-currency").andExpect(status().isBadRequest());
        api.postPayment(admin, merchantId, "5.00", "JPY", "unsupported").andExpect(status().isUnprocessableContent());
        api.postPayment(admin, UUID.randomUUID(), "5.00", "USD", "no-merchant").andExpect(status().isNotFound());

        assertThat(jdbc.sql("select count(*) from payments").query(Long.class).single()).isEqualTo(before);
    }

    @Test
    void attemptCrudKeepsPaymentAttemptsTableInSync() throws Exception {
        UUID paymentId = api.createPayment(admin, merchantId, "10.00", "USD", "order-attempts");

        String created = mockMvc.perform(post("/api/v1/payments/{id}/attempts", paymentId)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": "liqpay", "result": "SUBMITTED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.attemptNumber").value(2))
                .andReturn().getResponse().getContentAsString();
        UUID attemptId = UUID.fromString(JsonPath.read(created, "$.id"));

        Map<String, Object> row = attempt(attemptId);
        assertThat(row).containsEntry("payment_id", paymentId)
                .containsEntry("provider_id", "liqpay")
                .containsEntry("attempt_number", 2)
                .containsEntry("result", "SUBMITTED");
        assertThat(attempts(paymentId)).hasSize(2);

        mockMvc.perform(get("/api/v1/payments/{id}/attempts", paymentId)
                        .header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].id").value(attemptId.toString()));
        mockMvc.perform(get("/api/v1/payments/{id}/attempts/{attemptId}", paymentId, attemptId)
                        .header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value("liqpay"));

        mockMvc.perform(patch("/api/v1/payments/{id}/attempts/{attemptId}", paymentId, attemptId)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"result": "FAILED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("FAILED"));
        assertThat(attempt(attemptId)).containsEntry("result", "FAILED");

        mockMvc.perform(delete("/api/v1/payments/{id}/attempts/{attemptId}", paymentId, attemptId)
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        assertThat(count("payment_attempts", attemptId)).isZero();
        assertThat(attempts(paymentId)).hasSize(1);
        assertThat(count("payments", paymentId)).isEqualTo(1);

        mockMvc.perform(get("/api/v1/payments/{id}/attempts/{attemptId}", paymentId, attemptId)
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/payments/{id}/attempts", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": "stripe", "result": "SUBMITTED"}
                                """))
                .andExpect(status().isNotFound());
    }

    private Map<String, Object> payment(UUID id) {
        return jdbc.sql("select * from payments where id = ?").param(id).query().singleRow();
    }

    private Map<String, Object> attempt(UUID id) {
        return jdbc.sql("select * from payment_attempts where id = ?").param(id).query().singleRow();
    }

    private List<Map<String, Object>> attempts(UUID paymentId) {
        return jdbc.sql("select * from payment_attempts where payment_id = ? order by attempt_number")
                .param(paymentId).query().listOfRows();
    }

    private long count(String table, UUID id) {
        return jdbc.sql("select count(*) from " + table + " where id = ?").param(id).query(Long.class).single();
    }
}
