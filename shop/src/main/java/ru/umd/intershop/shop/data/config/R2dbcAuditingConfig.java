package ru.umd.intershop.shop.data.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import reactor.core.publisher.Mono;

@Configuration
@EnableR2dbcAuditing
public class R2dbcAuditingConfig {
    @Bean
    public ReactiveAuditorAware<String> auditorAware() {
        return () -> Mono.just("system");
    }
}