package ru.umd.intershop.shop.service.order.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.umd.intershop.shop.common.constant.OrderStatusEnum;
import ru.umd.intershop.shop.data.entity.OrderEntity;
import ru.umd.intershop.shop.data.entity.OrderItemEntity;
import ru.umd.intershop.shop.service.dto.*;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderServiceMapper {
    public OrderDto map(
        OrderEntity orderEntity,
        List<OrderItemEntity> orderItemEntities,
        List<ItemDto> itemDtos
    ) {
        return OrderDto
            .builder()
            .id(orderEntity.getId())
            .status(OrderStatusEnum.valueOf(orderEntity.getStatus()))
            .totalPrice(orderEntity.getTotalPrice())
            .items(
                orderItemEntities
                    .stream()
                    .map(
                        orderItemEntity -> OrderItemDto
                            .builder()
                            .item(
                                itemDtos
                                    .stream()
                                    .filter(itemEntity -> orderItemEntity.getItemId().equals(itemEntity.getId()))
                                    .findAny()
                                    .orElseThrow()
                            )
                            .count(orderItemEntity.getCount())
                            .build()
                    )
                    .toList()
            )
            .build();
    }
}
