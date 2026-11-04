package ukma.jpay.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class JpayApi {

    private final MockMvc mockMvc;

    public JpayApi(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public UUID createMerchant(String bearer, String... providerIds) throws Exception {
        String providers = String.join("\",\"", providerIds);
        String body = mockMvc.perform(post("/api/v1/merchants")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "IT Shop", "contactEmail": "it-%s@example.com",
                                 "defaultCurrency": "USD", "providerIds": ["%s"]}
                                """.formatted(UUID.randomUUID(), providers)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    public UUID createPayment(String bearer, UUID merchantId, String amount, String currency, String reference)
            throws Exception {
        String body = postPayment(bearer, merchantId, amount, currency, reference)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    public ResultActions postPayment(String bearer, UUID merchantId, String amount, String currency,
                                     String reference) throws Exception {
        return mockMvc.perform(post("/api/v1/payments")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"merchantId": "%s", "amount": %s, "currency": "%s", "merchantReference": "%s"}
                        """.formatted(merchantId, amount, currency, reference)));
    }

    public ResultActions webhook(String bearer, String providerId, UUID paymentId, String type) throws Exception {
        return mockMvc.perform(post("/api/v1/providers/{providerId}/webhook-events", providerId)
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"eventId": "evt-%s", "paymentId": "%s", "type": "%s"}
                        """.formatted(UUID.randomUUID(), paymentId, type)));
    }
}
