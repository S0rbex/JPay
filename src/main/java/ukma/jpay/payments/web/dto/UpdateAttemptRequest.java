package ukma.jpay.payments.web.dto;

import jakarta.validation.constraints.NotNull;
import ukma.jpay.payments.persistence.AttemptResult;

public record UpdateAttemptRequest(@NotNull AttemptResult result) {
}
