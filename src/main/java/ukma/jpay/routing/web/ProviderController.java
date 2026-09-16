package ukma.jpay.routing.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ukma.jpay.common.web.ApiPaths;
import ukma.jpay.routing.service.ProviderRouter;
import ukma.jpay.routing.web.dto.ProviderResponse;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.PROVIDERS)
public class ProviderController {

    private final ProviderRouter providerRouter;

    public ProviderController(ProviderRouter providerRouter) {
        this.providerRouter = providerRouter;
    }

    @GetMapping
    public List<ProviderResponse> list(@RequestParam(name = "currency", required = false) String currency) {
        return providerRouter.listProviders(currency).stream()
                .map(ProviderResponse::from)
                .toList();
    }
}
