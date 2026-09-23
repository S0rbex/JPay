package ukma.jpay.payments.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ukma.jpay.payments.domain.Payment;

import java.util.Set;

@Order(1)
@Component
class StripeMockProviderClient implements PaymentProviderClient {

    private static final Logger log = LoggerFactory.getLogger(StripeMockProviderClient.class);
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR");

    @Override
    public String providerId() {
        return "stripe";
    }

    @Override
    public boolean supports(String currency) {
        return SUPPORTED_CURRENCIES.contains(currency);
    }

    @Override
    public void submit(Payment payment) {
        log.info("Submitting payment {} ({} {}) to stripe", payment.id(), payment.amount(), payment.currency());
    }
}
