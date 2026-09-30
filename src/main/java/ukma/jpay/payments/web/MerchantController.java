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
import ukma.jpay.payments.domain.Merchant;
import ukma.jpay.payments.service.MerchantService;
import ukma.jpay.payments.web.dto.MerchantRequest;
import ukma.jpay.payments.web.dto.MerchantResponse;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    public ResponseEntity<MerchantResponse> create(@RequestBody @Valid MerchantRequest request) {
        Merchant merchant = merchantService.create(request.name(), request.contactEmail(),
                request.defaultCurrency(), request.providerIds());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{merchantId}")
                .buildAndExpand(merchant.id())
                .toUri();

        return ResponseEntity.created(location).body(MerchantResponse.from(merchant));
    }

    @GetMapping
    public List<MerchantResponse> list() {
        return merchantService.list().stream().map(MerchantResponse::from).toList();
    }

    @GetMapping("/{merchantId}")
    public MerchantResponse get(@PathVariable UUID merchantId) {
        return MerchantResponse.from(merchantService.get(merchantId));
    }

    @PutMapping("/{merchantId}")
    public MerchantResponse update(@PathVariable UUID merchantId,
                                   @RequestBody @Valid MerchantRequest request) {
        return MerchantResponse.from(merchantService.update(merchantId, request.name(),
                request.contactEmail(), request.defaultCurrency(), request.providerIds()));
    }

    @DeleteMapping("/{merchantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID merchantId) {
        merchantService.delete(merchantId);
    }
}
