package ukma.jpay.payments.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.service.PaymentService;
import ukma.jpay.payments.web.dto.CreatePaymentRequest;
import ukma.jpay.payments.web.dto.PaymentResponse;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Створення та перегляд платежів")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Створити платіж")
    @ApiResponse(responseCode = "201", description = "Платіж створено зі статусом INITIATED",
            headers = @Header(name = "Location", description = "Адреса створеного платежу",
                    schema = @Schema(type = "string", format = "uri")),
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class)))
    @ApiResponse(responseCode = "400", description = "Некоректний JSON, невідоме поле або помилка валідації",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PaymentResponse> create(@RequestBody @Valid CreatePaymentRequest request) {
        Payment payment = paymentService.create(
                request.amount(), request.currency(), request.merchantReference());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{paymentId}")
                .buildAndExpand(payment.id())
                .toUri();

        return ResponseEntity.created(location).body(PaymentResponse.from(payment));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Отримати платіж за ідентифікатором")
    @ApiResponse(responseCode = "200", description = "Поточний стан платежу",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class)))
    @ApiResponse(responseCode = "400", description = "Некоректний формат UUID",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Платіж не знайдено",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public PaymentResponse get(
            @Parameter(description = "Ідентифікатор платежу", example = "83ec3956-d8e0-4c88-8147-70787150617a")
            @PathVariable UUID paymentId) {
        return PaymentResponse.from(paymentService.get(paymentId));
    }
}
