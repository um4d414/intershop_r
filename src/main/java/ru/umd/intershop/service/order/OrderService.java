package ru.umd.intershop.service.order;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.umd.intershop.common.constant.CartItemAction;
import ru.umd.intershop.service.dto.OrderDto;

public interface OrderService {
    Mono<OrderDto> findById(Long id);

    Flux<OrderDto> findAllCompleted();

    Mono<OrderDto> getCart();

    Mono<Void> updateItemCount(Long id, CartItemAction action);

    Mono<Long> processCart();
}
