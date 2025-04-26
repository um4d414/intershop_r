package ru.umd.intershop.shop.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import ru.umd.intershop.client.ApiClient;
import ru.umd.intershop.client.api.PaymentsApi;

@Configuration
public class PaymentsClientConfig {
    @Value("${app.payments.service.url:http://localhost:8081}")
    private String paymentsServiceUrl;

    @Bean
    public ApiClient apiClient(WebClient.Builder webClientBuilder) {
        ApiClient apiClient = new ApiClient(webClientBuilder.build());
        apiClient.setBasePath(paymentsServiceUrl);
        return apiClient;
    }

    @Bean
    public PaymentsApi paymentsApi(ApiClient apiClient) {
        return new PaymentsApi(apiClient);
    }
}