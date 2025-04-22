package ru.umd.intershop.shop.data.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.umd.intershop.shop.data.entity.OrderEntity;

public interface OrderRepository extends ReactiveCrudRepository<OrderEntity, Long> {
    @NonNull
    Mono<OrderEntity> findById(@NonNull Long id);

    @NonNull
    Mono<Void> deleteById(@NonNull Long id);

    Mono<OrderEntity> findLastByStatus(String status);

    Flux<OrderEntity> findAllByStatus(String status);
}
