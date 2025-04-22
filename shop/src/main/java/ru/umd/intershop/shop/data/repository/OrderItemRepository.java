package ru.umd.intershop.shop.data.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.umd.intershop.shop.data.entity.OrderItemEntity;

public interface OrderItemRepository extends ReactiveCrudRepository<OrderItemEntity, Long> {
    Flux<OrderItemEntity> findAllByOrderIdOrderByIdDesc(Long orderId);
}
