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
class ProviderCrudIT {

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
    void createStoresProvider() throws Exception {
        String id = uniqueId();

        create(id, "Fondy", 5);

        Map<String, Object> row = jdbcClient.sql("SELECT name, priority FROM payment_providers WHERE id = ?")
                .param(id)
                .query()
                .singleRow();
        assertThat(row)
                .containsEntry("name", "Fondy")
                .containsEntry("priority", 5);
    }

    @Test
    void readReturnsSeededAndCreatedProviders() throws Exception {
        String id = uniqueId();
        create(id, "Monobank", 7);

        mockMvc.perform(get("/api/v1/providers/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Monobank"))
                .andExpect(jsonPath("$.priority").value(7));

        mockMvc.perform(get("/api/v1/providers").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem("stripe")))
                .andExpect(jsonPath("$[*].id", hasItem(id)));
    }

    @Test
    void updateChangesRow() throws Exception {
        String id = uniqueId();
        create(id, "Old Provider", 3);

        mockMvc.perform(put("/api/v1/providers/{id}", id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "New Provider", "priority": 9}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Provider"));

        Map<String, Object> row = jdbcClient.sql("SELECT name, priority FROM payment_providers WHERE id = ?")
                .param(id)
                .query()
                .singleRow();
        assertThat(row)
                .containsEntry("name", "New Provider")
                .containsEntry("priority", 9);
    }

    @Test
    void deleteRemovesRow() throws Exception {
        String id = uniqueId();
        create(id, "Temporary", 4);

        mockMvc.perform(delete("/api/v1/providers/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM payment_providers WHERE id = ?").param(id).query(Long.class).single())
                .isZero();
    }

    @Test
    void deleteOfUsedProviderKeepsRow() throws Exception {
        String id = uniqueId();
        create(id, "In Use", 6);
        mockMvc.perform(post("/api/v1/merchants").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Linked Shop",
                                  "contactEmail": "linked-%s@example.com",
                                  "defaultCurrency": "USD",
                                  "providerIds": ["%s"]
                                }
                                """.formatted(UUID.randomUUID(), id)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/providers/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());

        assertThat(jdbcClient.sql("SELECT COUNT(*) FROM payment_providers WHERE id = ?").param(id).query(Long.class).single())
                .isOne();
    }

    private void create(String id, String name, int priority) throws Exception {
        mockMvc.perform(post("/api/v1/providers").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": "%s", "name": "%s", "priority": %d}
                                """.formatted(id, name, priority)))
                .andExpect(status().isCreated());
    }

    private static String uniqueId() {
        return "p-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
