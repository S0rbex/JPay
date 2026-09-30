package ukma.jpay.payments.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.service.PaymentService;
import ukma.jpay.payments.web.dto.CreatePaymentRequest;
import ukma.jpay.payments.web.dto.PaymentDetailsResponse;
import ukma.jpay.payments.web.dto.PaymentResponse;
import ukma.jpay.payments.web.dto.UpdatePaymentRequest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@RequestBody @Valid CreatePaymentRequest request) {
        Payment payment = paymentService.create(
                request.merchantId(), request.amount(), request.currency(), request.merchantReference());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{paymentId}")
                .buildAndExpand(payment.id())
                .toUri();

        return ResponseEntity.created(location).body(PaymentResponse.from(payment));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable UUID paymentId) {
        return PaymentResponse.from(paymentService.get(paymentId));
    }

    @GetMapping
    public List<PaymentDetailsResponse> list() {
        return paymentService.list().stream().map(PaymentDetailsResponse::from).toList();
    }

    @PatchMapping("/{paymentId}")
    public PaymentResponse update(@PathVariable UUID paymentId,
                                  @RequestBody @Valid UpdatePaymentRequest request) {
        return PaymentResponse.from(paymentService.updateReference(paymentId, request.merchantReference()));
    }

    @DeleteMapping("/{paymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID paymentId) {
        paymentService.delete(paymentId);
    }
}
