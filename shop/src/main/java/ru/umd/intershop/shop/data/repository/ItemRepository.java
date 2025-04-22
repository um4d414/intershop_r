package ru.umd.intershop.shop.data.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.umd.intershop.shop.data.entity.ItemEntity;

import java.util.Collection;

public interface ItemRepository extends ReactiveCrudRepository<ItemEntity, Long> {
    Flux<ItemEntity> findAllByIsActiveTrue(Pageable pageable);

    @Query("""
           SELECT * FROM items i \
           WHERE i.is_active = true \
           AND (:search IS NULL OR :search = '' OR i.name ILIKE CONCAT('%', :search, '%'))
           """)
    Flux<ItemEntity> findAllByIsActiveTrueAndNameLikeIgnoreCase(Pageable pageable, String search);

    @Override
    @NonNull
    Mono<ItemEntity> findById(@NonNull Long id);

    Mono<Long> countByIsActiveTrue();

    @Query("""
           SELECT COUNT (*) FROM items i \
           WHERE i.is_active = true \
           AND (:search IS NULL OR :search = '' OR i.name ILIKE CONCAT('%', :search, '%'))
           """)
    Mono<Long> countByIsActiveTrueAndNameLikeIgnoreCase(String search);

    Flux<ItemEntity> findByIdIn(@Nullable Collection<Long> ids);
}
