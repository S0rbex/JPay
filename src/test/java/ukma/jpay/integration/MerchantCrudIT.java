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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class MerchantCrudIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    private String token;

    @BeforeEach
    void login() throws Exception {
        token = AuthTokens.bearer(mockMvc, "admin", "admin123");
    }

    @Test
    void createStoresMerchantAndProviders() throws Exception {
        String email = uniqueEmail();

        UUID id = create("Demo Shop", email, "USD", "stripe", "liqpay");

        Map<String, Object> row = jdbcClient.sql("SELECT name, contact_email, default_currency FROM merchants WHERE id = ?")
                .param(id)
                .query()
                .singleRow();
        assertThat(row)
                .containsEntry("name", "Demo Shop")
                .containsEntry("contact_email", email)
                .containsEntry("default_currency", "USD");
        assertThat(providersOf(id)).containsExactly("liqpay", "stripe");
    }

    @Test
    void readReturnsMerchantFromTable() throws Exception {
        String email = uniqueEmail();
        UUID id = create("Read Shop", email, "EUR", "stripe");

        mockMvc.perform(get("/api/v1/merchants/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Read Shop"))
                .andExpect(jsonPath("$.contactEmail").value(email))
                .andExpect(jsonPath("$.providerIds[0]").value("stripe"));

        mockMvc.perform(get("/api/v1/merchants").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id.toString())));
    }

    @Test
    void updateChangesRowAndProviders() throws Exception {
        UUID id = create("Old Name", uniqueEmail(), "USD", "stripe");
        String newEmail = uniqueEmail();

        mockMvc.perform(put("/api/v1/merchants/{id}", id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("New Name", newEmail, "UAH", "liqpay")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));

        Map<String, Object> row = jdbcClient.sql("SELECT name, contact_email, default_currency FROM merchants WHERE id = ?")
                .param(id)
                .query()
                .singleRow();
        assertThat(row)
                .containsEntry("name", "New Name")
                .containsEntry("contact_email", newEmail)
                .containsEntry("default_currency", "UAH");
        assertThat(providersOf(id)).containsExactly("liqpay");
    }

    @Test
    void deleteRemovesMerchantAndLinks() throws Exception {
        UUID id = create("Delete Shop", uniqueEmail(), "USD", "stripe", "liqpay");

        mockMvc.perform(delete("/api/v1/merchants/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM merchants WHERE id = ?").param(id).query(Long.class).single())
                .isZero();
        assertThat(providersOf(id)).isEmpty();
    }

    @Test
    void createWithInvalidBodyDoesNotTouchTable() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/v1/merchants").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", email, "usd", "stripe")))
                .andExpect(status().isBadRequest());

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM merchants WHERE contact_email = ?").param(email).query(Long.class).single())
                .isZero();
    }

    private UUID create(String name, String email, String currency, String... providerIds) throws Exception {
        String location = mockMvc.perform(post("/api/v1/merchants").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, email, currency, providerIds)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    private List<String> providersOf(UUID merchantId) {
        return jdbcClient.sql("SELECT provider_id FROM merchant_providers WHERE merchant_id = ? ORDER BY provider_id")
                .param(merchantId)
                .query(String.class)
                .list();
    }

    private static String body(String name, String email, String currency, String... providerIds) {
        String providers = String.join("\", \"", providerIds);
        return """
                {
                  "name": "%s",
                  "contactEmail": "%s",
                  "defaultCurrency": "%s",
                  "providerIds": ["%s"]
                }
                """.formatted(name, email, currency, providers);
    }

    private static String uniqueEmail() {
        return "shop-" + UUID.randomUUID() + "@example.com";
    }
}
