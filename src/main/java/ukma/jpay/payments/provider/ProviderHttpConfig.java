package ukma.jpay.payments.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(ProviderHttpProperties.class)
class ProviderHttpConfig {

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "jpay.providers.stripe.mode", havingValue = "http")
    StripeHttpProviderClient stripeHttpProviderClient(ProviderHttpProperties properties) {
        return new StripeHttpProviderClient(restClient(properties.endpoint("stripe")));
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(name = "jpay.providers.liqpay.mode", havingValue = "http")
    LiqPayHttpProviderClient liqPayHttpProviderClient(ProviderHttpProperties properties) {
        return new LiqPayHttpProviderClient(restClient(properties.endpoint("liqpay")));
    }

    static RestClient restClient(ProviderHttpProperties.Endpoint endpoint) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(endpoint.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(endpoint.readTimeout());
        return RestClient.builder().baseUrl(endpoint.baseUrl()).requestFactory(requestFactory).build();
    }
}
