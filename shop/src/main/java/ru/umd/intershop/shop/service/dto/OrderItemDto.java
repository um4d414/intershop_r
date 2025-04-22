package ru.umd.intershop.shop.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemDto {
    private ItemDto item;

    private Integer count;
}
