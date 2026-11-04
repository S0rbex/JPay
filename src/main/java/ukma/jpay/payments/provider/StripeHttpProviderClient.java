package ukma.jpay.payments.provider;

import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestClient;

import java.util.Set;

@Order(1)
class StripeHttpProviderClient extends HttpProviderClient {

    StripeHttpProviderClient(RestClient restClient) {
        super("stripe", Set.of("USD", "EUR"), restClient);
    }
}
