package ukma.jpay.payments.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ukma.jpay.payments.domain.Payment;

import java.util.Set;

@Order(2)
@Component
class LiqPayMockProviderClient implements PaymentProviderClient {

    private static final Logger log = LoggerFactory.getLogger(LiqPayMockProviderClient.class);
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("UAH", "USD");

    @Override
    public String providerId() {
        return "liqpay";
    }

    @Override
    public boolean supports(String currency) {
        return SUPPORTED_CURRENCIES.contains(currency);
    }

    @Override
    public void submit(Payment payment) {
        log.info("Submitting payment {} ({} {}) to liqpay", payment.id(), payment.amount(), payment.currency());
    }
}
