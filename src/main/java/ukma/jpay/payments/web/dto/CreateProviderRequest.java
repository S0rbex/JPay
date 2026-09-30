package ukma.jpay.payments.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProviderRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[a-z0-9-]+$", message = "id may contain lowercase letters, digits and hyphens")
        String id,

        @NotBlank
        @Size(max = 120)
        String name,

        @Min(1)
        int priority) {
}
