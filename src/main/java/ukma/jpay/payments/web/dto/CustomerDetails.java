package ukma.jpay.payments.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CustomerDetails(
        @Email @Size(max = 255) String email,
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$") String phone,
        @Pattern(regexp = "^[A-Z]{2}$") String countryCode) {
}
