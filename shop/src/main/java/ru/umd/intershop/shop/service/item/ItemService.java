package ru.umd.intershop.shop.service.item;

import org.springframework.data.domain.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.umd.intershop.shop.common.constant.ItemSortingEnum;
import ru.umd.intershop.shop.service.dto.ItemDto;
import ru.umd.intershop.shop.service.dto.ItemPageDto;

import java.util.List;

public interface ItemService {
    Mono<ItemDto> findById(Long id);

    Mono<ItemPageDto> findAllActive(Pageable pageable, ItemSortingEnum sort, String search);

    Flux<ItemDto> findByIds(List<Long> itemIds);
}
