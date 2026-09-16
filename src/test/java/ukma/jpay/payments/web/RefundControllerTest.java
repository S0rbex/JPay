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
import ukma.jpay.payments.domain.Refund;
import ukma.jpay.payments.domain.RefundStatus;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.error.InvalidStateTransitionException;
import ukma.jpay.payments.error.RefundAmountExceededException;
import ukma.jpay.payments.service.RefundService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RefundController.class)
class RefundControllerTest {

    private static final MerchantId MERCHANT_ID = new MerchantId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final Merchant MERCHANT =
            new Merchant(MERCHANT_ID, "Test Merchant", "test-key", MerchantStatus.ACTIVE, "USD");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RefundService refundService;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @MockitoBean
    private MerchantService merchantService;

    @BeforeEach
    void setUp() {
        when(merchantService.resolveActiveMerchant(anyString())).thenReturn(MERCHANT);
    }

    @SuppressWarnings("unchecked")
    private void stubIdempotencyToExecuteAction() {
        when(idempotencyService.executeIdempotent(anyString(), any(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> {
                    Supplier<IdempotentPayload<Object>> action = invocation.getArgument(4);
                    IdempotentPayload<Object> payload = action.get();
                    return new IdempotentExecution<>(payload.value(), payload.resourceId(), false);
                });
    }

    @Test
    void create_returns201AndUsesPathPaymentId() throws Exception {
        stubIdempotencyToExecuteAction();
        UUID paymentId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        when(refundService.refund(any(), org.mockito.ArgumentMatchers.eq(paymentId), any()))
                .thenReturn(new Refund(refundId, paymentId, new BigDecimal("19.99"), "USD",
                        RefundStatus.SUCCEEDED, "requested by customer", Instant.now()));

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refunds")
                        .header("X-Api-Key", "test-key")
                        .header("Idempotency-Key", "key-004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":19.99,\"currency\":\"USD\",\"reason\":\"requested by customer\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/payments/" + paymentId + "/refunds/" + refundId)))
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()));
    }

    @Test
    void create_whenPaymentNotSucceeded_returns409() throws Exception {
        stubIdempotencyToExecuteAction();
        UUID paymentId = UUID.randomUUID();
        when(refundService.refund(any(), org.mockito.ArgumentMatchers.eq(paymentId), any()))
                .thenThrow(new InvalidStateTransitionException(
                        paymentId, TransactionStatus.FAILED, TransactionStatus.REFUNDED, Set.of()));

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refunds")
                        .header("X-Api-Key", "test-key")
                        .header("Idempotency-Key", "key-005")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":19.99,\"currency\":\"USD\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", endsWith("invalid-state-transition")))
                .andExpect(jsonPath("$.currentStatus").value("FAILED"))
                .andExpect(jsonPath("$.requestedStatus").value("REFUNDED"))
                .andExpect(jsonPath("$.allowedTransitions").isArray());
    }

    @Test
    void create_whenAmountExceedsRefundable_returns422() throws Exception {
        stubIdempotencyToExecuteAction();
        UUID paymentId = UUID.randomUUID();
        when(refundService.refund(any(), org.mockito.ArgumentMatchers.eq(paymentId), any()))
                .thenThrow(new RefundAmountExceededException(
                        paymentId, new BigDecimal("10.00"), new BigDecimal("19.99"), "USD"));

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refunds")
                        .header("X-Api-Key", "test-key")
                        .header("Idempotency-Key", "key-006")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":19.99,\"currency\":\"USD\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type", endsWith("refund-amount-exceeds-refundable")))
                .andExpect(jsonPath("$.refundableAmount").value(10.00))
                .andExpect(jsonPath("$.requestedAmount").value(19.99));
    }

    @Test
    void create_whenAmountNegative_returns400_andServiceNeverCalled() throws Exception {
        UUID paymentId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refunds")
                        .header("X-Api-Key", "test-key")
                        .header("Idempotency-Key", "key-007")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-5,\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(refundService);
        verifyNoInteractions(idempotencyService);
    }
}
