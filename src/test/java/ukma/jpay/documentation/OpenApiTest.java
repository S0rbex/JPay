package ukma.jpay.documentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ukma.jpay.shared.notification.NotificationPublisher;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationPublisher notificationPublisher;

    @Test
    void documentsAllEndpointsAndModelExample() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("JPay API"))
                .andExpect(jsonPath("$.paths['/api/v1/payments'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/payments'].post.responses['400'].content['application/problem+json']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/payments/{paymentId}'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/payments/{paymentId}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/providers/{providerId}/webhook-events'].post.responses['202']").exists())
                .andExpect(jsonPath("$.components.schemas.CreatePaymentRequest.example.amount").value(19.99))
                .andExpect(jsonPath("$.components.schemas.CreatePaymentRequest.example.currency").value("USD"));
    }

    @Test
    void servesSwaggerUiAndItsConfiguration() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/swagger-ui/index.html")));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }
}
