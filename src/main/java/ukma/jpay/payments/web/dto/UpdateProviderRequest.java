package ukma.jpay.payments.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProviderRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @Min(1)
        int priority) {
}
