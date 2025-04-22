package ru.umd.intershop.shop.service.order;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.umd.intershop.shop.common.constant.CartItemAction;
import ru.umd.intershop.shop.common.constant.OrderStatusEnum;
import ru.umd.intershop.shop.data.entity.OrderEntity;
import ru.umd.intershop.shop.data.entity.OrderItemEntity;
import ru.umd.intershop.shop.data.repository.OrderItemRepository;
import ru.umd.intershop.shop.data.repository.OrderRepository;
import ru.umd.intershop.shop.service.dto.ItemDto;
import ru.umd.intershop.shop.service.dto.OrderDto;
import ru.umd.intershop.shop.service.exception.NotFoundException;
import ru.umd.intershop.shop.service.item.ItemService;
import ru.umd.intershop.shop.service.order.mapper.OrderServiceMapper;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultOrderService implements OrderService {
    private final OrderRepository orderRepository;

    private final ItemService itemService;

    private final OrderItemRepository orderItemRepository;

    private final OrderServiceMapper orderServiceMapper;

    @Override
    public Mono<OrderDto> findById(Long id) {
        return orderRepository.findById(id)
            .switchIfEmpty(Mono.error(new NotFoundException("Не найден заказ id=" + id)))
            .flatMap(this::assembleOrder);
    }

    @Override
    public Flux<OrderDto> findAllCompleted() {
        return orderRepository.findAllByStatus(OrderStatusEnum.COMPLETED.name())
            .flatMap(this::assembleOrder);
    }

    @Override
    public Mono<OrderDto> getCart() {
        return getCartEntity()
            .flatMap(this::assembleOrder);
    }

    @Override
    @Transactional
    public Mono<Void> updateItemCount(Long id, CartItemAction action) {
        return getCartEntity().flatMap(
            cart ->
                orderItemRepository.findAllByOrderIdOrderByIdDesc(cart.getId())
                    .filter(orderItemEntity -> orderItemEntity.getItemId().equals(id))
                    .next()
                    .switchIfEmpty(
                        itemService.findById(id)
                            .switchIfEmpty(Mono.error(new RuntimeException("Item not found")))
                            .flatMap(
                                itemDto ->
                                    orderItemRepository.save(
                                        OrderItemEntity.builder()
                                            .count(0)
                                            .itemId(itemDto.getId())
                                            .orderId(cart.getId())
                                            .build()
                                    )
                            )
                    )
                    .flatMap(orderItem -> {
                        // Обновляем orderItem в зависимости от действия
                        switch (action) {
                            case PLUS -> orderItem.setCount(orderItem.getCount() + 1);
                            case MINUS -> {
                                orderItem.setCount(orderItem.getCount() - 1);
                                if (orderItem.getCount() < 0) {
                                    orderItem.setCount(0);
                                }
                            }
                            case DELETE -> {
                                return orderItemRepository.delete(orderItem);
                            }
                        }
                        return orderItemRepository.save(orderItem).then();
                    })
        );
    }

    @Override
    @Transactional
    public Mono<Long> processCart() {
        return getCartEntity().flatMap(
            cart ->
                orderItemRepository.findAllByOrderIdOrderByIdDesc(cart.getId())
                    .flatMap(orderItem ->
                                 itemService.findById(orderItem.getItemId())
                                     .map(itemDto -> itemDto.getPrice()
                                         .multiply(BigDecimal.valueOf(orderItem.getCount())))
                    )
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .flatMap(totalPrice -> {
                        cart.setTotalPrice(totalPrice);
                        cart.setStatus(OrderStatusEnum.COMPLETED.name());
                        return orderRepository.save(cart);
                    })
                    .map(OrderEntity::getId)
        );
    }

    private Mono<OrderDto> assembleOrder(OrderEntity orderEntity) {
        Flux<OrderItemEntity> orderItemsFlux = orderItemRepository
            .findAllByOrderIdOrderByIdDesc(orderEntity.getId())
            .cache();

        Mono<List<OrderItemEntity>> orderItemsMono = orderItemsFlux.collectList();

        Mono<List<ItemDto>> itemsMono = orderItemsFlux
            .map(OrderItemEntity::getItemId)
            .collectList()
            .flatMap(itemIds -> itemService.findByIds(itemIds).collectList());

        return Mono.zip(Mono.just(orderEntity), orderItemsMono, itemsMono)
            .map(tuple -> {
                OrderEntity order = tuple.getT1();
                List<OrderItemEntity> orderItems = tuple.getT2();
                List<ItemDto> itemDtos = tuple.getT3();
                return orderServiceMapper.map(order, orderItems, itemDtos);
            });
    }

    private Mono<OrderEntity> getCartEntity() {
        return orderRepository.findLastByStatus(OrderStatusEnum.NEW.name())
            .switchIfEmpty(
                Mono.defer(() -> orderRepository.save(new OrderEntity(OrderStatusEnum.NEW.name(), BigDecimal.ZERO)))
                    .onErrorResume(
                        DataIntegrityViolationException.class, e ->
                            orderRepository.findLastByStatus(OrderStatusEnum.NEW.name())
                    )
            );
    }
}
