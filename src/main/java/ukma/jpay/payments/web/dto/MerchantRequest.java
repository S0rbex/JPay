package ukma.jpay.payments.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MerchantRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @NotBlank
        @Email
        @Size(max = 254)
        String contactEmail,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "defaultCurrency must contain three uppercase letters")
        String defaultCurrency,

        @NotNull
        List<@NotBlank String> providerIds) {
}
