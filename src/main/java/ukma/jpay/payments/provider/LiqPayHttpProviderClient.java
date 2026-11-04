package ukma.jpay.payments.provider;

import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestClient;

import java.util.Set;

@Order(2)
class LiqPayHttpProviderClient extends HttpProviderClient {

    LiqPayHttpProviderClient(RestClient restClient) {
        super("liqpay", Set.of("UAH", "USD"), restClient);
    }
}
