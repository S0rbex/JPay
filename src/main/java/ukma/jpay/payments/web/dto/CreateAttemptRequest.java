package ukma.jpay.payments.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ukma.jpay.payments.persistence.AttemptResult;

public record CreateAttemptRequest(
        @NotBlank
        String providerId,

        @NotNull
        AttemptResult result) {
}
