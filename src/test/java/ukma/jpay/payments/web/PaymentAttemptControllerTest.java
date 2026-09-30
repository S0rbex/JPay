package ukma.jpay.payments.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ukma.jpay.payments.domain.PaymentAttempt;
import ukma.jpay.payments.error.AttemptNotFoundException;
import ukma.jpay.payments.error.PaymentNotFoundException;
import ukma.jpay.payments.error.ProviderNotFoundException;
import ukma.jpay.payments.persistence.AttemptResult;
import ukma.jpay.payments.service.PaymentAttemptService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentAttemptController.class)
class PaymentAttemptControllerTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID ATTEMPT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentAttemptService attemptService;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(attemptService.create(PAYMENT_ID, "liqpay", AttemptResult.FAILED))
                .thenReturn(attempt(2, "liqpay", AttemptResult.FAILED));

        mockMvc.perform(post("/api/v1/payments/{paymentId}/attempts", PAYMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": "liqpay", "result": "FAILED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        endsWith("/api/v1/payments/" + PAYMENT_ID + "/attempts/" + ATTEMPT_ID)))
                .andExpect(jsonPath("$.attemptNumber").value(2))
                .andExpect(jsonPath("$.providerId").value("liqpay"))
                .andExpect(jsonPath("$.result").value("FAILED"));
    }

    @Test
    void createWithInvalidBodyReturnsValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/payments/{paymentId}/attempts", PAYMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:validation-failed"))
                .andExpect(jsonPath("$.errors.length()").value(2));

        verifyNoInteractions(attemptService);
    }

    @Test
    void createWithUnknownResultIsRejectedAsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/payments/{paymentId}/attempts", PAYMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": "liqpay", "result": "MAYBE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:malformed-json"));

        verifyNoInteractions(attemptService);
    }

    @Test
    void createForMissingPaymentReturns404() throws Exception {
        when(attemptService.create(PAYMENT_ID, "liqpay", AttemptResult.SUBMITTED))
                .thenThrow(new PaymentNotFoundException(PAYMENT_ID));

        mockMvc.perform(post("/api/v1/payments/{paymentId}/attempts", PAYMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": "liqpay", "result": "SUBMITTED"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:payment-not-found"));
    }

    @Test
    void createWithUnknownProviderReturns404() throws Exception {
        when(attemptService.create(PAYMENT_ID, "ghost", AttemptResult.SUBMITTED))
                .thenThrow(new ProviderNotFoundException("ghost"));

        mockMvc.perform(post("/api/v1/payments/{paymentId}/attempts", PAYMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId": "ghost", "result": "SUBMITTED"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:provider-not-found"));
    }

    @Test
    void listReturnsAttempts() throws Exception {
        when(attemptService.list(PAYMENT_ID)).thenReturn(List.of(
                attempt(1, "liqpay", AttemptResult.FAILED), attempt(2, "stripe", AttemptResult.SUBMITTED)));

        mockMvc.perform(get("/api/v1/payments/{paymentId}/attempts", PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].providerId").value("stripe"));
    }

    @Test
    void getReturnsAttempt() throws Exception {
        when(attemptService.get(PAYMENT_ID, ATTEMPT_ID)).thenReturn(attempt(1, "stripe", AttemptResult.SUBMITTED));

        mockMvc.perform(get("/api/v1/payments/{paymentId}/attempts/{attemptId}", PAYMENT_ID, ATTEMPT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ATTEMPT_ID.toString()))
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()));
    }

    @Test
    void getOnMissingAttemptReturns404() throws Exception {
        when(attemptService.get(PAYMENT_ID, ATTEMPT_ID)).thenThrow(new AttemptNotFoundException(PAYMENT_ID, ATTEMPT_ID));

        mockMvc.perform(get("/api/v1/payments/{paymentId}/attempts/{attemptId}", PAYMENT_ID, ATTEMPT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:attempt-not-found"))
                .andExpect(jsonPath("$.attemptId").value(ATTEMPT_ID.toString()));
    }

    @Test
    void patchUpdatesResult() throws Exception {
        when(attemptService.updateResult(PAYMENT_ID, ATTEMPT_ID, AttemptResult.SUCCEEDED))
                .thenReturn(attempt(1, "stripe", AttemptResult.SUCCEEDED));

        mockMvc.perform(patch("/api/v1/payments/{paymentId}/attempts/{attemptId}", PAYMENT_ID, ATTEMPT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"result": "SUCCEEDED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCEEDED"));
    }

    @Test
    void patchWithoutResultReturnsValidationProblem() throws Exception {
        mockMvc.perform(patch("/api/v1/payments/{paymentId}/attempts/{attemptId}", PAYMENT_ID, ATTEMPT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("result"));

        verifyNoInteractions(attemptService);
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/payments/{paymentId}/attempts/{attemptId}", PAYMENT_ID, ATTEMPT_ID))
                .andExpect(status().isNoContent());

        verify(attemptService).delete(PAYMENT_ID, ATTEMPT_ID);
    }

    private static PaymentAttempt attempt(int number, String providerId, AttemptResult result) {
        return new PaymentAttempt(ATTEMPT_ID, PAYMENT_ID, providerId, number, result,
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
