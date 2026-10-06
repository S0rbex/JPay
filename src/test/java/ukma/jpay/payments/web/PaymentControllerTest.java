package ukma.jpay.payments.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.error.PaymentNotFoundException;
import ukma.jpay.payments.service.PaymentService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void createReturns201AndDelegatesToService() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment(paymentId);
        when(paymentService.create(any(), eq("USD"), eq("order-1"))).thenReturn(payment);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 19.99,
                                  "currency": "USD",
                                  "merchantReference": "order-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", endsWith("/api/v1/payments/" + paymentId)))
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("INITIATED"))
                .andExpect(jsonPath("$.amount").value(19.99));

        verify(paymentService).create(new BigDecimal("19.99"), "USD", "order-1");
    }

    @Test
    void getReturnsPayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(paymentService.get(paymentId)).thenReturn(payment(paymentId));

        mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.currency").value("USD"));

        verify(paymentService).get(paymentId);
    }

    @Test
    void invalidRequestReturnsValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 0,
                                  "currency": "usd"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:validation-failed"))
                .andExpect(jsonPath("$.errors").isArray());

        verifyNoInteractions(paymentService);
    }

    @Test
    void unknownJsonFieldIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 19.99,
                                  "currency": "USD",
                                  "unexpected": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:unknown-field"))
                .andExpect(jsonPath("$.unknownField").value("unexpected"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void malformedJsonReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:malformed-json"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void businessExceptionReturnsProblemDetail() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(paymentService.get(paymentId)).thenThrow(new PaymentNotFoundException(paymentId));

        mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:payment-not-found"))
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()));
    }

    private static Payment payment(UUID paymentId) {
        return new Payment(
                paymentId,
                TransactionStatus.INITIATED,
                new BigDecimal("19.99"),
                "USD",
                "order-1",
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
