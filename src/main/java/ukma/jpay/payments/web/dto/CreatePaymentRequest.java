package ukma.jpay.payments.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Запит створення платежу", example = """
        {"amount":19.99,"currency":"USD","merchantReference":"order-1"}
        """)
public record CreatePaymentRequest(
        @Schema(description = "Сума платежу", example = "19.99")
        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        @Digits(integer = 12, fraction = 2)
        BigDecimal amount,

        @Schema(description = "Трилітерний код валюти", example = "USD")
        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must contain three uppercase letters")
        String currency,

        @Schema(description = "Ідентифікатор замовлення продавця", example = "order-1")
        @Size(max = 64)
        String merchantReference) {
}
