package ukma.jpay.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import ukma.jpay.payments.domain.PaymentStatusChanged;

@Component
class MerchantNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(MerchantNotificationListener.class);

    @ApplicationModuleListener
    void on(PaymentStatusChanged event) {
        log.info("notify merchant: payment {} {} -> {}", event.paymentId(), event.from(), event.to());
    }
}
