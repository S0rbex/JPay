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
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.service.CreatePaymentCommand;
import ukma.jpay.payments.service.PaymentService;
import ukma.jpay.payments.web.dto.AttemptResponse;
import ukma.jpay.payments.web.dto.CreatePaymentRequest;
import ukma.jpay.payments.web.dto.CustomerDetails;
import ukma.jpay.payments.web.dto.PaymentResponse;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static ukma.jpay.common.web.Fingerprints.sha256;

@RestController
@Validated
@RequestMapping(ApiPaths.PAYMENTS)
public class PaymentController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String REPLAYED_HEADER = "Idempotency-Replayed";
    private static final String SCOPE = "payment";

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public PaymentController(PaymentService paymentService, IdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader(name = IDEMPOTENCY_HEADER)
            @NotBlank @Size(min = 8, max = 255) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String idempotencyKey,
            @RequestBody @Valid CreatePaymentRequest request,
            @CurrentMerchant MerchantId merchant) {

        String fingerprint = fingerprint(merchant, request);
        IdempotentExecution<PaymentResponse> execution = idempotencyService.executeIdempotent(
                SCOPE, merchant, idempotencyKey, fingerprint,
                () -> {
                    Payment payment = paymentService.create(merchant, toCommand(request));
                    return new IdempotentPayload<>(payment.id().toString(), PaymentResponse.from(payment));
                });

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(ApiPaths.PAYMENTS + "/{id}")
                .buildAndExpand(execution.resourceId())
                .toUri();

        return ResponseEntity
                .status(execution.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .location(location)
                .header(REPLAYED_HEADER, String.valueOf(execution.replayed()))
                .body(execution.value());
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable UUID paymentId, @CurrentMerchant MerchantId merchant) {
        return PaymentResponse.from(paymentService.get(merchant, paymentId));
    }

    @GetMapping("/{paymentId}/attempts")
    public List<AttemptResponse> attempts(@PathVariable UUID paymentId, @CurrentMerchant MerchantId merchant) {
        return paymentService.listAttempts(merchant, paymentId).stream()
                .map(AttemptResponse::from)
                .toList();
    }

    private static CreatePaymentCommand toCommand(CreatePaymentRequest request) {
        CustomerDetails customer = request.customer();
        return new CreatePaymentCommand(
                request.amount(), request.currency(), request.merchantReference(), request.description(),
                customer == null ? null : customer.email(),
                customer == null ? null : customer.phone(),
                customer == null ? null : customer.countryCode());
    }

    private static String fingerprint(MerchantId merchantId, CreatePaymentRequest request) {
        String raw = merchantId + "|" + request.amount().stripTrailingZeros().toPlainString() + "|"
                + request.currency() + "|" + Objects.toString(request.merchantReference(), "");
        return sha256(raw);
    }
}
