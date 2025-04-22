package ru.umd.intershop.shop.service.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemPageDto {
    private List<ItemDto> itemList;

    private int totalItems;

    private int totalPages;

    private int page;

    private int pageSize;
}
