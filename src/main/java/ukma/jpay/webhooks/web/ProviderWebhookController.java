package ukma.jpay.webhooks.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ukma.jpay.payments.service.PaymentService;
import ukma.jpay.webhooks.web.dto.ProviderWebhookRequest;
import ukma.jpay.webhooks.web.dto.WebhookAcknowledgement;

@RestController
@RequestMapping("/api/v1/providers/{providerId}/webhook-events")
public class ProviderWebhookController {

    private final PaymentService paymentService;

    public ProviderWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<WebhookAcknowledgement> receive(
            @PathVariable String providerId,
            @RequestBody @Valid ProviderWebhookRequest request) {

        paymentService.updateStatus(request.paymentId(), providerId, request.type().targetStatus());

        return ResponseEntity.accepted()
                .body(new WebhookAcknowledgement(providerId, request.eventId(), request.paymentId()));
    }
}
