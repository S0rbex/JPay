package ukma.jpay.common.error;

import org.springframework.http.HttpStatus;

import java.net.URI;

public enum ProblemType {

    VALIDATION_FAILED("validation-failed", "Request validation failed", HttpStatus.BAD_REQUEST),
    UNKNOWN_FIELD("unknown-field", "Unknown field in request body", HttpStatus.BAD_REQUEST),
    MALFORMED_JSON("malformed-json", "Malformed request body", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_FOUND("payment-not-found", "Payment not found", HttpStatus.NOT_FOUND),
    PROVIDER_MISMATCH("provider-mismatch", "Payment provider mismatch", HttpStatus.CONFLICT),
    INVALID_STATE_TRANSITION("invalid-state-transition", "Invalid payment state transition", HttpStatus.CONFLICT),
    UNSUPPORTED_CURRENCY("unsupported-currency", "Unsupported currency", HttpStatus.UNPROCESSABLE_CONTENT),
    MERCHANT_NOT_FOUND("merchant-not-found", "Merchant not found", HttpStatus.NOT_FOUND),
    PROVIDER_NOT_FOUND("provider-not-found", "Provider not found", HttpStatus.NOT_FOUND),
    ATTEMPT_NOT_FOUND("attempt-not-found", "Payment attempt not found", HttpStatus.NOT_FOUND),
    DUPLICATE_MERCHANT_EMAIL("duplicate-merchant-email", "Merchant email already in use", HttpStatus.CONFLICT),
    DUPLICATE_PROVIDER("duplicate-provider", "Provider already exists", HttpStatus.CONFLICT),
    RESOURCE_IN_USE("resource-in-use", "Resource is still in use", HttpStatus.CONFLICT),
    PAYMENT_NOT_DELETABLE("payment-not-deletable", "Payment cannot be deleted", HttpStatus.CONFLICT);

    private static final String BASE_URI = "urn:jpay:problem:";

    private final String slug;
    private final String title;
    private final HttpStatus status;

    ProblemType(String slug, String title, HttpStatus status) {
        this.slug = slug;
        this.title = title;
        this.status = status;
    }

    public String title() {
        return title;
    }

    public HttpStatus status() {
        return status;
    }

    public URI uri() {
        return URI.create(BASE_URI + slug);
    }
}
