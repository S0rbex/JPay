package ukma.jpay.payments.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ukma.jpay.idempotency.service.IdempotencyService;
import ukma.jpay.idempotency.service.IdempotentExecution;
import ukma.jpay.idempotency.service.IdempotentPayload;
import ukma.jpay.merchants.domain.Merchant;
import ukma.jpay.merchants.domain.MerchantId;
import ukma.jpay.merchants.domain.MerchantStatus;
import ukma.jpay.merchants.service.MerchantService;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.error.PaymentNotFoundException;
import ukma.jpay.payments.service.PaymentService;
import ukma.jpay.routing.error.NoEligibleProviderException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
class PaymentControllerTest {

    private static final MerchantId MERCHANT_ID = new MerchantId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final Merchant MERCHANT =
            new Merchant(MERCHANT_ID, "Test Merchant", "test-key", MerchantStatus.ACTIVE, "USD");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @MockitoBean
    private MerchantService merchantService;

    @BeforeEach
    void setUp() {
        when(merchantService.resolveActiveMerchant(anyString())).thenReturn(MERCHANT);
    }

    @SuppressWarnings("unchecked")
    private void stubIdempotencyToExecuteAction(boolean replayed) {
        when(idempotencyService.executeIdempotent(anyString(), any(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> {
                    Supplier<IdempotentPayload<Object>> action = invocation.getArgument(4);
                    IdempotentPayload<Object> payload = action.get();
                    return new IdempotentExecution<>(payload.value(), payload.resourceId(), replayed);
                });
    }

    private static Payment succeededPayment(UUID id) {
        Instant now = Instant.now();
        return new Payment(id, MERCHANT_ID, TransactionStatus.SUCCEEDED, new BigDecimal("19.99"), "USD",
                "stripe", "Stripe", "order-1", null, null, BigDecimal.ZERO, now, now);
    }

    @Test
    void create_returns201WithLocationAndBody() throws Exception {
        stubIdempotencyToExecuteAction(false);
        UUID paymentId = UUID.randomUUID();
        when(paymentService.create(any(), any())).thenReturn(succeededPayment(paymentId));

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Api-Key", "test-key")
                        .header("Idempotency-Key", "key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":19.99,\"currency\":\"USD\",\"merchantReference\":\"order-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/payments/" + paymentId)))
                .andExpect(header().string("Idempotency-Replayed", "false"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.amount").value(19.99));
    }

    @Test
    void create_whenServiceReportsReplay_returns200AndReplayHeader() throws Exception {
        stubIdempotencyToExecuteAction(true);
        UUID paymentId = UUID.randomUUID();
        when(paymentService.create(any(), any())).thenReturn(succeededPayment(paymentId));

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Api-Key", "test-key")
                        .header("Idempotency-Key", "key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":19.99,\"currency\":\"USD\",\"merchantReference\":\"order-1\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/payments/" + paymentId)));
    }

    @Test
    void create_whenBodyInvalid_returns400_andServiceNeverCalled() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Api-Key", "test-key")
                        .header("Idempotency-Key", "key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0,\"currency\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", org.hamcrest.Matchers.endsWith("validation-failed")))
                .andExpect(jsonPath("$.errors").isArray());

        verifyNoInteractions(paymentService);
        verifyNoInteractions(idempotencyService);
    }

    @Test
    void create_whenUnknownField_returns400UnknownField() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Api-Key", "test-key")
                        .header("Idempotency-Key", "key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountt\":10,\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", org.hamcrest.Matchers.endsWith("unknown-field")))
                .andExpect(jsonPath("$.unknownField").value("amountt"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void create_whenMalformedJson_returns400MalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Api-Key", "test-key")
                        .header("Idempotency-Key", "key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", org.hamcrest.Matchers.endsWith("malformed-json")));

        verifyNoInteractions(paymentService);
    }

    @Test
    void create_whenIdempotencyKeyHeaderMissing_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Api-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":19.99,\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void create_whenNoEligibleProvider_returns503() throws Exception {
        stubIdempotencyToExecuteAction(false);
        when(paymentService.create(any(), any()))
                .thenThrow(new NoEligibleProviderException("USD", List.of("stripe", "adyen")));

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-Api-Key", "test-key")
                        .header("Idempotency-Key", "key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":19.99,\"currency\":\"USD\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.type", org.hamcrest.Matchers.endsWith("no-eligible-provider")));
    }

    @Test
    void get_whenNotFound_returns404ProblemDetail() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(paymentService.get(any(), org.mockito.ArgumentMatchers.eq(paymentId)))
                .thenThrow(new PaymentNotFoundException(paymentId));

        mockMvc.perform(get("/api/v1/payments/" + paymentId).header("X-Api-Key", "test-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", org.hamcrest.Matchers.endsWith("payment-not-found")))
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.instance").value("/api/v1/payments/" + paymentId));
    }
}
