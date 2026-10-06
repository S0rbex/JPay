package ukma.jpay.webhooks.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ukma.jpay.payments.domain.TransactionStatus;
import ukma.jpay.payments.service.PaymentService;
import ukma.jpay.webhooks.domain.ProviderEventType;
import ukma.jpay.webhooks.web.dto.ProviderWebhookRequest;
import ukma.jpay.webhooks.web.dto.WebhookAcknowledgement;

@RestController
@RequestMapping("/api/v1/providers/{providerId}/webhook-events")
@Tag(name = "Provider webhooks", description = "Події платіжних провайдерів")
public class ProviderWebhookController {

    private final PaymentService paymentService;

    public ProviderWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Прийняти подію провайдера",
            description = "PAYMENT_SUCCEEDED змінює статус платежу на SUCCEEDED, PAYMENT_FAILED — на FAILED")
    @ApiResponse(responseCode = "202", description = "Подію прийнято, статус платежу оновлено",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WebhookAcknowledgement.class)))
    @ApiResponse(responseCode = "400", description = "Некоректний JSON, невідоме поле або помилка валідації",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Платіж не знайдено",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<WebhookAcknowledgement> receive(
            @Parameter(description = "Ідентифікатор провайдера", example = "stripe")
            @PathVariable String providerId,
            @RequestBody @Valid ProviderWebhookRequest request) {

        TransactionStatus status = request.type() == ProviderEventType.PAYMENT_SUCCEEDED
                ? TransactionStatus.SUCCEEDED
                : TransactionStatus.FAILED;
        paymentService.updateStatus(request.paymentId(), status);

        return ResponseEntity.accepted()
                .body(new WebhookAcknowledgement(providerId, request.eventId(), request.paymentId()));
    }
}
