package ukma.jpay.payments.web;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import ukma.jpay.payments.web.dto.PaymentResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jpay-http-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF"
})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class PaymentPersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void startsWithDemoDataReturnsDtosInOneQueryAndPreservesPaymentAndWebhookFlow() throws Exception {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].merchant.name").value("Demo Shop"))
                .andExpect(jsonPath("$[0].provider.name").value("Stripe"))
                .andExpect(jsonPath("$[0].attempts.length()").value(2))
                .andExpect(jsonPath("$[0].attempts[0].provider.name").value("LiqPay"))
                .andExpect(jsonPath("$[0].attempts[1].attemptNumber").value(2))
                .andExpect(jsonPath("$[1].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$[2].attempts").isEmpty())
                .andExpect(jsonPath("$[2].merchant").isEmpty());
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);

        var response = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 19.99, "currency": "USD", "merchantReference": "http-order"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readValue(response, PaymentResponse.class).id().toString();

        mockMvc.perform(post("/api/v1/providers/stripe/webhook-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": "integration-event", "paymentId": "%s", "type": "PAYMENT_SUCCEEDED"}
                                """.formatted(paymentId)))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/providers/stripe/webhook-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": "integration-event", "paymentId": "%s", "type": "PAYMENT_SUCCEEDED"}
                                """.formatted(paymentId)))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.merchantReference").value("http-order"));
    }
}
