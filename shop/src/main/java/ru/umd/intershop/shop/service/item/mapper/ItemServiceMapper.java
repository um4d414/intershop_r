package ru.umd.intershop.shop.service.item.mapper;

import org.springframework.stereotype.Component;
import ru.umd.intershop.shop.data.entity.ItemEntity;
import ru.umd.intershop.shop.service.dto.ItemDto;

@Component
public class ItemServiceMapper {
    public ItemDto map(ItemEntity itemEntity) {
        return ItemDto
            .builder()
            .id(itemEntity.getId())
            .name(itemEntity.getName())
            .price(itemEntity.getPrice())
            .description(itemEntity.getDescription())
            .imageFileName(itemEntity.getImageFileName())
            .build();
    }
}