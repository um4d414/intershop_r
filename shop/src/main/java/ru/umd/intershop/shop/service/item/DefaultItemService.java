package ru.umd.intershop.shop.service.item;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.umd.intershop.shop.common.constant.ItemSortingEnum;
import ru.umd.intershop.shop.data.entity.ItemEntity;
import ru.umd.intershop.shop.data.repository.ItemRepository;
import ru.umd.intershop.shop.service.dto.ItemDto;
import ru.umd.intershop.shop.service.dto.ItemPageDto;
import ru.umd.intershop.shop.service.exception.NotFoundException;
import ru.umd.intershop.shop.service.item.mapper.ItemServiceMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultItemService implements ItemService {
    private final ItemRepository itemRepository;

    private final ItemServiceMapper itemServiceMapper;

    @Override
    public Mono<ItemDto> findById(Long id) {
        return itemRepository
            .findById(id)
            .map(itemServiceMapper::map)
            .switchIfEmpty(Mono.error(new NotFoundException("Не найден продукт id=" + id)));
    }

    @Override
    public Mono<ItemPageDto> findAllActive(
        Pageable pageable,
        ItemSortingEnum sort,
        String search
    ) {
        var pageRequest = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(sort.getEntityField()).ascending()
        );

        Flux<ItemEntity> itemEntities = StringUtils.hasText(search) ?
            itemRepository.findAllByIsActiveTrueAndNameLikeIgnoreCase(pageRequest, search) :
            itemRepository.findAllByIsActiveTrue(pageRequest);

        Mono<Long> itemCount = StringUtils.hasText(search) ?
            itemRepository.countByIsActiveTrueAndNameLikeIgnoreCase(search) :
            itemRepository.countByIsActiveTrue();

        return Mono.zip(
                itemEntities
                    .map(itemServiceMapper::map)
                    .collectList(),
                itemCount
            )
            .map(
                tuple -> {
                    long totalItems = tuple.getT2();
                    long pageSize = pageable.getPageSize();
                    long totalPages = (totalItems + pageSize - 1) / pageSize;
                    long currentPage = pageable.getPageNumber();

                    return ItemPageDto.builder()
                        .itemList(tuple.getT1())
                        .totalItems((int) totalItems)
                        .totalPages((int) totalPages)
                        .page((int) currentPage)
                        .pageSize((int) pageSize)
                        .build();
                }
            );
    }

    @Override
    public Flux<ItemDto> findByIds(List<Long> itemIds) {
        return itemRepository
            .findByIdIn(itemIds)
            .map(itemServiceMapper::map);
    }
}
