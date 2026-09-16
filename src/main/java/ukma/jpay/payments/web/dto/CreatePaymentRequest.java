package ukma.jpay.payments.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ukma.jpay.common.validation.ConsistentMoneyScale;
import ukma.jpay.common.validation.Monetary;
import ukma.jpay.common.validation.SupportedCurrency;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = false)
@ConsistentMoneyScale
public record CreatePaymentRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        @Digits(integer = 12, fraction = 3)
        BigDecimal amount,

        @NotBlank @SupportedCurrency String currency,

        @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9._:-]*$") String merchantReference,
        @Size(max = 255) String description,
        @Valid CustomerDetails customer) implements Monetary {
}
