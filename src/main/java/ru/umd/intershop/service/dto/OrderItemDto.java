package ru.umd.intershop.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemDto {
    private ItemDto item;

    private Integer count;
}
