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
import ukma.jpay.payments.domain.PaymentAttempt;
import ukma.jpay.payments.service.PaymentAttemptService;
import ukma.jpay.payments.web.dto.AttemptResponse;
import ukma.jpay.payments.web.dto.CreateAttemptRequest;
import ukma.jpay.payments.web.dto.UpdateAttemptRequest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/{paymentId}/attempts")
public class PaymentAttemptController {

    private final PaymentAttemptService attemptService;

    public PaymentAttemptController(PaymentAttemptService attemptService) {
        this.attemptService = attemptService;
    }

    @PostMapping
    public ResponseEntity<AttemptResponse> create(@PathVariable UUID paymentId,
                                                  @RequestBody @Valid CreateAttemptRequest request) {
        PaymentAttempt attempt = attemptService.create(paymentId, request.providerId(), request.result());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{attemptId}")
                .buildAndExpand(attempt.id())
                .toUri();

        return ResponseEntity.created(location).body(AttemptResponse.from(attempt));
    }

    @GetMapping
    public List<AttemptResponse> list(@PathVariable UUID paymentId) {
        return attemptService.list(paymentId).stream().map(AttemptResponse::from).toList();
    }

    @GetMapping("/{attemptId}")
    public AttemptResponse get(@PathVariable UUID paymentId, @PathVariable UUID attemptId) {
        return AttemptResponse.from(attemptService.get(paymentId, attemptId));
    }

    @PatchMapping("/{attemptId}")
    public AttemptResponse update(@PathVariable UUID paymentId, @PathVariable UUID attemptId,
                                  @RequestBody @Valid UpdateAttemptRequest request) {
        return AttemptResponse.from(attemptService.updateResult(paymentId, attemptId, request.result()));
    }

    @DeleteMapping("/{attemptId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID paymentId, @PathVariable UUID attemptId) {
        attemptService.delete(paymentId, attemptId);
    }
}
