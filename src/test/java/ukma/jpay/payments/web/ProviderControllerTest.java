package ukma.jpay.payments.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ukma.jpay.payments.domain.PaymentProvider;
import ukma.jpay.payments.error.DuplicateProviderException;
import ukma.jpay.payments.error.ProviderNotFoundException;
import ukma.jpay.payments.error.ResourceInUseException;
import ukma.jpay.payments.service.ProviderService;

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
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

@WebMvcTest(ProviderController.class)
class ProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProviderService providerService;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(providerService.create("adyen", "Adyen", 3)).thenReturn(new PaymentProvider("adyen", "Adyen", 3));

        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": "adyen", "name": "Adyen", "priority": 3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/providers/adyen")))
                .andExpect(jsonPath("$.id").value("adyen"))
                .andExpect(jsonPath("$.priority").value(3));
    }

    @Test
    void createWithInvalidBodyReturnsValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": "Bad Id", "name": "", "priority": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:validation-failed"))
                .andExpect(jsonPath("$.errors.length()").value(3));

        verifyNoInteractions(providerService);
    }

    @Test
    void createDuplicateReturns409() throws Exception {
        when(providerService.create("stripe", "Stripe", 1)).thenThrow(new DuplicateProviderException("stripe"));

        mockMvc.perform(post("/api/v1/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": "stripe", "name": "Stripe", "priority": 1}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:duplicate-provider"))
                .andExpect(jsonPath("$.providerId").value("stripe"));
    }

    @Test
    void listReturnsProviders() throws Exception {
        when(providerService.list()).thenReturn(List.of(
                new PaymentProvider("stripe", "Stripe", 1), new PaymentProvider("liqpay", "LiqPay", 2)));

        mockMvc.perform(get("/api/v1/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].name").value("LiqPay"));
    }

    @Test
    void getReturnsProvider() throws Exception {
        when(providerService.get("stripe")).thenReturn(new PaymentProvider("stripe", "Stripe", 1));

        mockMvc.perform(get("/api/v1/providers/{providerId}", "stripe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("stripe"));
    }

    @Test
    void getOnMissingProviderReturns404() throws Exception {
        when(providerService.get("ghost")).thenThrow(new ProviderNotFoundException("ghost"));

        mockMvc.perform(get("/api/v1/providers/{providerId}", "ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:provider-not-found"))
                .andExpect(jsonPath("$.providerId").value("ghost"));
    }

    @Test
    void putUpdatesProvider() throws Exception {
        when(providerService.update("stripe", "Stripe EU", 5))
                .thenReturn(new PaymentProvider("stripe", "Stripe EU", 5));

        mockMvc.perform(put("/api/v1/providers/{providerId}", "stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Stripe EU", "priority": 5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Stripe EU"))
                .andExpect(jsonPath("$.priority").value(5));
    }

    @Test
    void putWithInvalidBodyReturnsValidationProblem() throws Exception {
        mockMvc.perform(put("/api/v1/providers/{providerId}", "stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Stripe", "priority": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("priority"));

        verifyNoInteractions(providerService);
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/providers/{providerId}", "adyen"))
                .andExpect(status().isNoContent());

        verify(providerService).delete("adyen");
    }

    @Test
    void deleteOfReferencedProviderReturns409() throws Exception {
        doThrow(new ResourceInUseException("Provider", "stripe")).when(providerService).delete("stripe");

        mockMvc.perform(delete("/api/v1/providers/{providerId}", "stripe"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:jpay:problem:resource-in-use"))
                .andExpect(jsonPath("$.resourceId").value("stripe"));
    }
}
