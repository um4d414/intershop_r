package ru.umd.intershop.shop.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import ru.umd.intershop.client.ApiClient;
import ru.umd.intershop.client.api.PaymentsApi;

@Configuration
public class PaymentsClientConfig {
    @Bean
    public ApiClient apiClient(WebClient.Builder webClientBuilder) {
        return new ApiClient(webClientBuilder.build());
    }

    @Bean
    public PaymentsApi paymentsApi(ApiClient apiClient) {
        return new PaymentsApi(apiClient);
    }
}
