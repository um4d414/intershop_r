package ru.umd.intershop.service.dto;

import lombok.Builder;
import lombok.Data;
import ru.umd.intershop.common.constant.OrderStatusEnum;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderDto {
    private Long id;

    private OrderStatusEnum status;

    private BigDecimal totalPrice;

    private List<OrderItemDto> items;
}
