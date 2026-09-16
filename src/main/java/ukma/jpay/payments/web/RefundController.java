package ukma.jpay.payments.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ukma.jpay.common.web.ApiPaths;
import ukma.jpay.idempotency.service.IdempotencyService;
import ukma.jpay.idempotency.service.IdempotentExecution;
import ukma.jpay.idempotency.service.IdempotentPayload;
import ukma.jpay.merchants.domain.MerchantId;
import ukma.jpay.merchants.web.CurrentMerchant;
import ukma.jpay.payments.domain.Refund;
import ukma.jpay.payments.service.CreateRefundCommand;
import ukma.jpay.payments.service.RefundService;
import ukma.jpay.payments.web.dto.CreateRefundRequest;
import ukma.jpay.payments.web.dto.RefundResponse;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static ukma.jpay.common.web.Fingerprints.sha256;

@RestController
@Validated
@RequestMapping(ApiPaths.PAYMENTS + "/{paymentId}/refunds")
public class RefundController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String REPLAYED_HEADER = "Idempotency-Replayed";
    private static final String SCOPE = "refund";

    private final RefundService refundService;
    private final IdempotencyService idempotencyService;

    public RefundController(RefundService refundService, IdempotencyService idempotencyService) {
        this.refundService = refundService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<RefundResponse> create(
            @PathVariable UUID paymentId,
            @RequestHeader(name = IDEMPOTENCY_HEADER)
            @NotBlank @Size(min = 8, max = 255) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String idempotencyKey,
            @RequestBody @Valid CreateRefundRequest request,
            @CurrentMerchant MerchantId merchant) {

        String fingerprint = fingerprint(paymentId, request);
        IdempotentExecution<RefundResponse> execution = idempotencyService.executeIdempotent(
                SCOPE, merchant, idempotencyKey, fingerprint,
                () -> {
                    Refund refund = refundService.refund(merchant, paymentId,
                            new CreateRefundCommand(request.amount(), request.currency(), request.reason()));
                    return new IdempotentPayload<>(refund.id().toString(), RefundResponse.from(refund));
                });

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(ApiPaths.PAYMENTS + "/{paymentId}/refunds/{refundId}")
                .buildAndExpand(paymentId, execution.resourceId())
                .toUri();

        return ResponseEntity
                .status(execution.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .location(location)
                .header(REPLAYED_HEADER, String.valueOf(execution.replayed()))
                .body(execution.value());
    }

    @GetMapping("/{refundId}")
    public RefundResponse get(
            @PathVariable UUID paymentId, @PathVariable UUID refundId, @CurrentMerchant MerchantId merchant) {
        return RefundResponse.from(refundService.getRefund(merchant, paymentId, refundId));
    }

    @GetMapping
    public List<RefundResponse> list(@PathVariable UUID paymentId, @CurrentMerchant MerchantId merchant) {
        return refundService.listRefunds(merchant, paymentId).stream()
                .map(RefundResponse::from)
                .toList();
    }

    private static String fingerprint(UUID paymentId, CreateRefundRequest request) {
        String raw = paymentId + "|" + request.amount().stripTrailingZeros().toPlainString() + "|" + request.currency();
        return sha256(raw);
    }
}
