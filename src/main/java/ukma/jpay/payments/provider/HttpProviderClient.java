package ukma.jpay.payments.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ukma.jpay.payments.domain.Payment;
import ukma.jpay.payments.error.ProviderRejectedException;
import ukma.jpay.payments.error.ProviderTimeoutException;
import ukma.jpay.payments.error.ProviderUnavailableException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Set;
import java.util.UUID;

abstract class HttpProviderClient implements PaymentProviderClient {

    private static final Logger log = LoggerFactory.getLogger(HttpProviderClient.class);

    private final String providerId;
    private final Set<String> supportedCurrencies;
    private final RestClient restClient;

    HttpProviderClient(String providerId, Set<String> supportedCurrencies, RestClient restClient) {
        this.providerId = providerId;
        this.supportedCurrencies = supportedCurrencies;
        this.restClient = restClient;
    }

    @Override
    public String providerId() {
        return providerId;
    }

    @Override
    public boolean supports(String currency) {
        return supportedCurrencies.contains(currency);
    }

    @Override
    public void submit(Payment payment) {
        log.info("Submitting payment {} ({} {}) to {} over HTTP",
                payment.id(), payment.amount(), payment.currency(), providerId);
        try {
            restClient.post()
                    .uri("/v1/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SubmitRequest(payment.id(), payment.amount(), payment.currency(),
                            payment.merchantReference()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                throw new ProviderRejectedException(providerId, e.getStatusCode().value());
            }
            throw new ProviderUnavailableException(providerId, "HTTP " + e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                throw new ProviderTimeoutException(providerId);
            }
            throw new ProviderUnavailableException(providerId, e.getMessage());
        }
    }

    private static boolean isTimeout(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }

    record SubmitRequest(UUID paymentId, BigDecimal amount, String currency, String merchantReference) {
    }
}
