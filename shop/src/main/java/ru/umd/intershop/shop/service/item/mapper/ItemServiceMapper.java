package ru.umd.intershop.shop.service.item.mapper;

import org.springframework.stereotype.Component;
import ru.umd.intershop.shop.common.constant.ItemSortingEnum;
import ru.umd.intershop.shop.data.cache.model.ItemCacheModel;
import ru.umd.intershop.shop.data.cache.model.ItemPageCacheModel;
import ru.umd.intershop.shop.data.entity.ItemEntity;
import ru.umd.intershop.shop.service.dto.ItemDto;
import ru.umd.intershop.shop.service.dto.ItemPageDto;

import java.util.List;
import java.util.stream.Collectors;

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

    public ItemDto mapCacheModelToDto(ItemCacheModel model) {
        return ItemDto.builder()
            .id(Long.parseLong(model.id()))
            .name(model.name())
            .price(model.price())
            .description(model.description())
            .imageFileName(model.imageFileName())
            .build();
    }

    public ItemCacheModel mapDtoToCacheModel(ItemDto dto) {
        return new ItemCacheModel(
            dto.getId().toString(),
            dto.getName(),
            dto.getPrice(),
            dto.getDescription(),
            dto.getImageFileName()
        );
    }

    public ItemPageCacheModel mapToPageCacheModel(ItemPageDto pageDto, ItemSortingEnum sort, String search) {
        List<ItemCacheModel> cacheItems = pageDto.getItemList().stream()
            .map(this::mapDtoToCacheModel)
            .collect(Collectors.toList());

        return ItemPageCacheModel.create(
            cacheItems,
            pageDto.getTotalItems(),
            pageDto.getTotalPages(),
            pageDto.getPage(),
            pageDto.getPageSize(),
            sort != null ? sort.name() : "NO",
            search
        );
    }

    public ItemPageDto mapFromPageCacheModel(ItemPageCacheModel cacheModel) {
        List<ItemDto> dtoItems = cacheModel.items().stream()
            .map(this::mapCacheModelToDto)
            .collect(Collectors.toList());

        return ItemPageDto.builder()
            .itemList(dtoItems)
            .totalItems(cacheModel.totalItems())
            .totalPages(cacheModel.totalPages())
            .page(cacheModel.page())
            .pageSize(cacheModel.pageSize())
            .build();
    }


}