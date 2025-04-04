package ru.umd.intershop.web.config.filter;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;

@Component
public class BackRedirectFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
            .then(Mono.defer(() -> {
                String location = exchange.getResponse().getHeaders().getFirst(HttpHeaders.LOCATION);
                if ("redirectBack".equals(location)) {
                    String referer = exchange.getRequest().getHeaders().getFirst(HttpHeaders.REFERER);
                    if (referer != null && !referer.isBlank()) {
                        exchange.getResponse().getHeaders().set(HttpHeaders.LOCATION, referer);
                    } else {
                        exchange.getResponse().getHeaders().set(HttpHeaders.LOCATION, "/main/items");
                    }
                }
                return Mono.empty();
            }));
    }
}