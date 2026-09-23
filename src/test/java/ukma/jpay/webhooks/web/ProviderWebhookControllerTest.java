package ukma.jpay.webhooks.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.error.InvalidStateTransitionException;
import ukma.jpay.payments.error.ProviderMismatchException;
import ukma.jpay.payments.service.PaymentService;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProviderWebhookController.class)
class ProviderWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void receiveReturns202AndUpdatesPaymentStatus() throws Exception {
        UUID paymentId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/providers/{providerId}/webhook-events", "stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "evt-1",
                                  "paymentId": "%s",
                                  "type": "PAYMENT_SUCCEEDED"
                                }
                                """.formatted(paymentId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.providerId").value("stripe"))
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()));

        verify(paymentService).updateStatus(paymentId, "stripe", TransactionStatus.SUCCEEDED);
    }

    @Test
    void invalidWebhookReturnsValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/providers/{providerId}/webhook-events", "stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:validation-failed"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void invalidStateTransitionReturnsConflictProblem() throws Exception {
        UUID paymentId = UUID.randomUUID();
        doThrow(new InvalidStateTransitionException(paymentId, TransactionStatus.INITIATED, TransactionStatus.FAILED))
                .when(paymentService).updateStatus(paymentId, "stripe", TransactionStatus.FAILED);

        mockMvc.perform(post("/api/v1/providers/{providerId}/webhook-events", "stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "evt-1",
                                  "paymentId": "%s",
                                  "type": "PAYMENT_FAILED"
                                }
                                """.formatted(paymentId)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:invalid-state-transition"));
    }

    @Test
    void webhookFromDifferentProviderReturnsConflictProblem() throws Exception {
        UUID paymentId = UUID.randomUUID();
        doThrow(new ProviderMismatchException(paymentId, "stripe", "liqpay"))
                .when(paymentService).updateStatus(paymentId, "liqpay", TransactionStatus.SUCCEEDED);

        mockMvc.perform(post("/api/v1/providers/{providerId}/webhook-events", "liqpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "evt-1",
                                  "paymentId": "%s",
                                  "type": "PAYMENT_SUCCEEDED"
                                }
                                """.formatted(paymentId)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:provider-mismatch"))
                .andExpect(jsonPath("$.expectedProviderId").value("stripe"))
                .andExpect(jsonPath("$.actualProviderId").value("liqpay"));
    }
}
