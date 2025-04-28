package ru.umd.intershop.shop.service.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.umd.intershop.shop.common.constant.ItemSortingEnum;
import ru.umd.intershop.shop.data.cache.model.ItemCacheModel;
import ru.umd.intershop.shop.data.cache.model.ItemPageCacheModel;
import ru.umd.intershop.shop.data.config.CacheProperties;
import ru.umd.intershop.shop.data.entity.ItemEntity;
import ru.umd.intershop.shop.data.repository.ItemRepository;
import ru.umd.intershop.shop.service.dto.ItemDto;
import ru.umd.intershop.shop.service.dto.ItemPageDto;
import ru.umd.intershop.shop.service.exception.NotFoundException;
import ru.umd.intershop.shop.service.item.mapper.ItemServiceMapper;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultItemService implements ItemService {
    private final ItemRepository itemRepository;

    private final ItemServiceMapper itemServiceMapper;

    private final ReactiveRedisOperations<String, ItemCacheModel> itemRedisOperations;

    private final ReactiveRedisOperations<String, ItemPageCacheModel> itemPageRedisOperations;

    private final CacheProperties cacheProperties;

    @Override
    public Mono<ItemDto> findById(Long id) {
        return itemRedisOperations
            .opsForValue()
            .get(id.toString())
            .map(itemServiceMapper::mapCacheModelToDto)
            .doOnSuccess(cached -> {
                if (cached != null) {
                    log.debug("Получены данные из кэша для ключа: {}", id);
                }
            })
            .onErrorResume(e -> {
                log.warn("Ошибка при получении товара из кэша: {}", e.getMessage());
                return Mono.empty();
            })
            .switchIfEmpty(
                itemRepository
                    .findById(id)
                    .map(itemServiceMapper::map)
                    .flatMap(
                        itemDto ->
                            itemRedisOperations
                                .opsForValue()
                                .set(id.toString(), itemServiceMapper.mapDtoToCacheModel(itemDto))
                                .onErrorResume(e -> {
                                    log.warn("Ошибка при сохранении в кэш: {}", e.getMessage());
                                    return Mono.just(true);
                                })
                                .thenReturn(itemDto)
                    )
                    .switchIfEmpty(Mono.error(new NotFoundException("Не найден продукт id=" + id)))
            );
    }

    @Override
    public Mono<ItemPageDto> findAllActive(
        Pageable pageable,
        ItemSortingEnum sort,
        String search
    ) {
        // Создаем ключ для кэша
        String cacheKey = generateItemPageCacheKey(pageable, sort, search);

        return itemPageRedisOperations
            .opsForValue()
            .get(cacheKey)
            .doOnSuccess(cached -> {
                if (cached != null) {
                    log.debug("Получены данные из кэша для ключа: {}", cacheKey);
                }
            })
            .map(itemServiceMapper::mapFromPageCacheModel)
            .onErrorResume(e -> {
                log.warn("Ошибка при получении страницы товаров из кэша: {}", e.getMessage());
                return Mono.empty();
            })
            .switchIfEmpty(
                fetchItemsFromDatabase(pageable, sort, search)
                    .flatMap(pageDto -> {
                        log.debug("Сохранение результатов в кэш для ключа: {}", cacheKey);
                        return itemPageRedisOperations
                            .opsForValue()
                            .set(
                                cacheKey,
                                itemServiceMapper.mapToPageCacheModel(pageDto, sort, search),
                                Duration.ofSeconds(cacheProperties.getTtl().getItemsPage())
                            )
                            .onErrorResume(e -> {
                                log.warn("Ошибка при сохранении страницы в кэш: {}", e.getMessage());
                                return Mono.just(true);
                            })
                            .thenReturn(pageDto);
                    })
            );
    }

    @Override
    public Flux<ItemDto> findByIds(List<Long> itemIds) {
        return itemRepository
            .findByIdIn(itemIds)
            .map(itemServiceMapper::map);
    }

    public Mono<Void> updateItemInPageCache(ItemDto updatedItem) {
        return itemPageRedisOperations
            .scan()
            .filter(key -> key.toString().startsWith("items:page:"))
            .flatMap(key ->
                         itemPageRedisOperations.opsForValue().get(key)
                             .filter(pageCache ->
                                         pageCache.items().stream()
                                             .anyMatch(item -> item.id().equals(updatedItem.getId()))
                             )
                             .flatMap(existingCache -> {
                                 List<ItemCacheModel> updatedItems = existingCache.items().stream()
                                     .map(item -> {
                                         if (item.id().equals(updatedItem.getId())) {
                                             return new ItemCacheModel(
                                                 updatedItem.getId().toString(),
                                                 updatedItem.getName(),
                                                 updatedItem.getPrice(),
                                                 updatedItem.getDescription(),
                                                 updatedItem.getImageFileName()
                                             );
                                         }
                                         return item;
                                     })
                                     .collect(Collectors.toList());

                                 ItemPageCacheModel updatedCache = new ItemPageCacheModel(
                                     updatedItems,
                                     existingCache.totalItems(),
                                     existingCache.totalPages(),
                                     existingCache.page(),
                                     existingCache.pageSize(),
                                     existingCache.sortField(),
                                     existingCache.searchQuery(),
                                     System.currentTimeMillis()
                                 );

                                 return itemPageRedisOperations.opsForValue()
                                     .set(key, updatedCache, Duration.ofSeconds(cacheProperties.getTtl().getItemsPage()));
                             })
            )
            .then();
    }


    private String generateItemPageCacheKey(Pageable pageable, ItemSortingEnum sort, String search) {
        return "items:page:" +
               pageable.getPageNumber() + ":" +
               pageable.getPageSize() + ":" +
               (sort != null ? sort.name() : "NO") + ":" +
               (StringUtils.hasText(search) ? search.replace(" ", "_") : "noSearch");
    }

    private Mono<ItemPageDto> fetchItemsFromDatabase(
        Pageable pageable,
        ItemSortingEnum sort,
        String search
    ) {
        log.debug("Получение страницы товаров из БД: page={}, size={}, sort={}, search={}",
                  pageable.getPageNumber(), pageable.getPageSize(), sort, search);

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

}
