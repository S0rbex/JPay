package ukma.jpay.payments.web.dto;

import jakarta.validation.constraints.Size;

public record UpdatePaymentRequest(@Size(max = 64) String merchantReference) {
}
