package ukma.jpay.payments.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ukma.jpay.payments.domain.PaymentProvider;
import ukma.jpay.payments.service.ProviderService;
import ukma.jpay.payments.web.dto.CreateProviderRequest;
import ukma.jpay.payments.web.dto.ProviderResponse;
import ukma.jpay.payments.web.dto.UpdateProviderRequest;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/providers")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping
    public ResponseEntity<ProviderResponse> create(@RequestBody @Valid CreateProviderRequest request) {
        PaymentProvider provider = providerService.create(
                request.id(), request.name(), request.priority());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{providerId}")
                .buildAndExpand(provider.id())
                .toUri();

        return ResponseEntity.created(location).body(ProviderResponse.from(provider));
    }

    @GetMapping
    public List<ProviderResponse> list() {
        return providerService.list().stream().map(ProviderResponse::from).toList();
    }

    @GetMapping("/{providerId}")
    public ProviderResponse get(@PathVariable String providerId) {
        return ProviderResponse.from(providerService.get(providerId));
    }

    @PutMapping("/{providerId}")
    public ProviderResponse update(@PathVariable String providerId,
                                   @RequestBody @Valid UpdateProviderRequest request) {
        return ProviderResponse.from(
                providerService.update(providerId, request.name(), request.priority()));
    }

    @DeleteMapping("/{providerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String providerId) {
        providerService.delete(providerId);
    }
}
