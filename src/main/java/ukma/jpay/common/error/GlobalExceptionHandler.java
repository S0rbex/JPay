package ukma.jpay.common.error;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException exception) {
        ProblemDetail problem = problemDetail(exception.type(), exception.getMessage());
        exception.properties().forEach(problem::setProperty);
        return ResponseEntity.status(exception.type().status()).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail problem = problemDetail(
                ProblemType.VALIDATION_FAILED, "Request validation failed");
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", Objects.requireNonNullElse(error.getDefaultMessage(), "invalid value")))
                .sorted(Comparator.comparing(error -> error.get("field")))
                .toList();
        problem.setProperty("errors", errors);

        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        Throwable cause = exception.getMostSpecificCause();
        ProblemDetail problem;
        if (cause instanceof UnrecognizedPropertyException unrecognized) {
            problem = problemDetail(
                    ProblemType.UNKNOWN_FIELD, "Unknown field: " + unrecognized.getPropertyName());
            problem.setProperty("unknownField", unrecognized.getPropertyName());
        } else {
            problem = problemDetail(ProblemType.MALFORMED_JSON, "Malformed JSON request body");
        }

        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static ProblemDetail problemDetail(ProblemType type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(type.status(), detail);
        problem.setTitle(type.title());
        problem.setType(type.uri());
        return problem;
    }
}
