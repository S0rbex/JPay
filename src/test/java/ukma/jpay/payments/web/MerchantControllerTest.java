package ukma.jpay.payments.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ukma.jpay.payments.domain.Merchant;
import ukma.jpay.payments.error.DuplicateMerchantEmailException;
import ukma.jpay.payments.error.MerchantNotFoundException;
import ukma.jpay.payments.error.ProviderNotFoundException;
import ukma.jpay.payments.error.ResourceInUseException;
import ukma.jpay.payments.service.MerchantService;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MerchantController.class)
class MerchantControllerTest {

    private static final String BODY = """
            {
              "name": "Demo Shop",
              "contactEmail": "shop@example.com",
              "defaultCurrency": "USD",
              "providerIds": ["stripe", "liqpay"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MerchantService merchantService;

    @Test
    void createReturns201WithLocation() throws Exception {
        UUID merchantId = UUID.randomUUID();
        when(merchantService.create("Demo Shop", "shop@example.com", "USD", List.of("stripe", "liqpay")))
                .thenReturn(merchant(merchantId));

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/merchants/" + merchantId)))
                .andExpect(jsonPath("$.id").value(merchantId.toString()))
                .andExpect(jsonPath("$.providerIds[0]").value("liqpay"));
    }

    @Test
    void createWithInvalidBodyReturnsValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "contactEmail": "not-an-email",
                                  "defaultCurrency": "usd"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:validation-failed"))
                .andExpect(jsonPath("$.errors.length()").value(4));

        verifyNoInteractions(merchantService);
    }

    @Test
    void createWithDuplicateEmailReturns409() throws Exception {
        when(merchantService.create(any(), any(), any(), any()))
                .thenThrow(new DuplicateMerchantEmailException("shop@example.com"));

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:duplicate-merchant-email"))
                .andExpect(jsonPath("$.contactEmail").value("shop@example.com"));
    }

    @Test
    void createWithUnknownProviderReturns404() throws Exception {
        when(merchantService.create(any(), any(), any(), any()))
                .thenThrow(new ProviderNotFoundException("stripe"));

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:provider-not-found"))
                .andExpect(jsonPath("$.providerId").value("stripe"));
    }

    @Test
    void listReturnsMerchants() throws Exception {
        when(merchantService.list()).thenReturn(List.of(merchant(UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/merchants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Demo Shop"));
    }

    @Test
    void getReturnsMerchant() throws Exception {
        UUID merchantId = UUID.randomUUID();
        when(merchantService.get(merchantId)).thenReturn(merchant(merchantId));

        mockMvc.perform(get("/api/v1/merchants/{merchantId}", merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(merchantId.toString()))
                .andExpect(jsonPath("$.defaultCurrency").value("USD"));
    }

    @Test
    void getOnMissingMerchantReturns404() throws Exception {
        UUID merchantId = UUID.randomUUID();
        when(merchantService.get(merchantId)).thenThrow(new MerchantNotFoundException(merchantId));

        mockMvc.perform(get("/api/v1/merchants/{merchantId}", merchantId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:merchant-not-found"))
                .andExpect(jsonPath("$.merchantId").value(merchantId.toString()));
    }

    @Test
    void putUpdatesMerchant() throws Exception {
        UUID merchantId = UUID.randomUUID();
        when(merchantService.update(eq(merchantId), eq("Demo Shop"), eq("shop@example.com"), eq("USD"), any()))
                .thenReturn(merchant(merchantId));

        mockMvc.perform(put("/api/v1/merchants/{merchantId}", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(merchantId.toString()));

        verify(merchantService).update(merchantId, "Demo Shop", "shop@example.com", "USD",
                List.of("stripe", "liqpay"));
    }

    @Test
    void putWithInvalidBodyReturnsValidationProblem() throws Exception {
        mockMvc.perform(put("/api/v1/merchants/{merchantId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:validation-failed"));

        verifyNoInteractions(merchantService);
    }

    @Test
    void deleteReturns204() throws Exception {
        UUID merchantId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/merchants/{merchantId}", merchantId))
                .andExpect(status().isNoContent());

        verify(merchantService).delete(merchantId);
    }

    @Test
    void deleteOfMerchantWithPaymentsReturns409() throws Exception {
        UUID merchantId = UUID.randomUUID();
        doThrow(new ResourceInUseException("Merchant", merchantId.toString()))
                .when(merchantService).delete(merchantId);

        mockMvc.perform(delete("/api/v1/merchants/{merchantId}", merchantId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:resource-in-use"))
                .andExpect(jsonPath("$.resource").value("Merchant"));
    }

    private static Merchant merchant(UUID merchantId) {
        return new Merchant(merchantId, "Demo Shop", "shop@example.com", "USD", List.of("liqpay", "stripe"));
    }
}
