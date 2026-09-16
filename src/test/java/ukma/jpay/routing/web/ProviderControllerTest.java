package ukma.jpay.routing.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ukma.jpay.merchants.service.MerchantService;
import ukma.jpay.routing.domain.PaymentProvider;
import ukma.jpay.routing.domain.ProviderHealth;
import ukma.jpay.routing.service.ProviderRouter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProviderController.class)
class ProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProviderRouter providerRouter;

    @MockitoBean
    private MerchantService merchantService;

    @Test
    void list_filtersByCurrency_passesFilterToService() throws Exception {
        when(providerRouter.listProviders("EUR")).thenReturn(List.of(
                new PaymentProvider("adyen", "Adyen", Set.of("EUR"), new BigDecimal("2.5"), 1, ProviderHealth.HEALTHY)));

        mockMvc.perform(get("/api/v1/providers").param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("adyen"));

        verify(providerRouter).listProviders("EUR");
    }
}
