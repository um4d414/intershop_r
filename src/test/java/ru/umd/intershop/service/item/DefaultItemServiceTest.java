package ru.umd.intershop.service.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.umd.intershop.common.constant.ItemSortingEnum;
import ru.umd.intershop.data.entity.ItemEntity;
import ru.umd.intershop.data.repository.ItemRepository;
import ru.umd.intershop.service.dto.ItemDto;
import ru.umd.intershop.service.dto.ItemPageDto;
import ru.umd.intershop.service.exception.NotFoundException;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class DefaultItemServiceTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private DefaultItemService defaultItemService;

    @BeforeEach
    public void setUp() {
        // Очищаем данные перед каждым тестом.
        itemRepository.deleteAll().block();
    }

    @Test
    public void testFindByIdFound() {
        // Создаём тестовый товар и сохраняем его.
        ItemEntity entity = ItemEntity.builder()
            .name("Test Item")
            .description("Test Description")
            .price(new BigDecimal("9.99"))
            .imageFileName("test.jpg")
            .isActive(true)
            .build();
        entity = itemRepository.save(entity).block();

        // Получаем товар из сервиса (Mono<ItemDto>).
        Mono<ItemDto> result = defaultItemService.findById(entity.getId());

        // Проверяем, что результат содержит ожидаемые значения.
        ItemEntity finalEntity = entity;
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertEquals(finalEntity.getId(), dto.getId());
                assertEquals(finalEntity.getName(), dto.getName());
                assertEquals(finalEntity.getDescription(), dto.getDescription());
                assertEquals(finalEntity.getPrice(), dto.getPrice());
                assertEquals(finalEntity.getImageFileName(), dto.getImageFileName());
            })
            .verifyComplete();
    }

    @Test
    public void testFindByIdNotFound() {
        Mono<ItemDto> result = defaultItemService.findById(999L);

        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                                    throwable instanceof NotFoundException &&
                                    throwable.getMessage().equals("Не найден продукт id=999")
            )
            .verify();
    }
    @Test
    public void testFindAllActiveWithoutSearch() {
        // Создаём два товара и сохраняем их.
        ItemEntity entity1 = ItemEntity.builder()
            .name("Alpha")
            .description("First item")
            .price(new BigDecimal("10.00"))
            .imageFileName("1.jpg")
            .isActive(true)
            .build();
        ItemEntity entity2 = ItemEntity.builder()
            .name("Beta")
            .description("Second item")
            .price(new BigDecimal("20.00"))
            .imageFileName("2.jpg")
            .isActive(true)
            .build();
        itemRepository.saveAll(Arrays.asList(entity1, entity2)).collectList().block();

        Pageable pageable = PageRequest.of(0, 10);
        Mono<ItemPageDto> result = defaultItemService.findAllActive(pageable, ItemSortingEnum.NO, null);

        StepVerifier.create(result)
            .assertNext(page -> {
                assertNotNull(page);
                assertEquals(2, page.getTotalItems(), "Должно быть 2 товара");
            })
            .verifyComplete();
    }

    @Test
    public void testFindAllActiveWithSearch() {
        // Создаём два товара, один из которых содержит в названии "Test".
        ItemEntity entity1 = ItemEntity.builder()
            .name("TestItem")
            .description("Test Description")
            .price(new BigDecimal("15.00"))
            .imageFileName("3.jpg")
            .isActive(true)
            .build();
        ItemEntity entity2 = ItemEntity.builder()
            .name("Other")
            .description("Other Description")
            .price(new BigDecimal("5.00"))
            .imageFileName("4.jpg")
            .isActive(true)
            .build();
        itemRepository.saveAll(Arrays.asList(entity1, entity2)).collectList().block();

        Pageable pageable = PageRequest.of(0, 10);
        String search = "Test";
        Mono<ItemPageDto> result = defaultItemService.findAllActive(pageable, ItemSortingEnum.NO, search);

        StepVerifier.create(result)
            .assertNext(page -> {
                assertNotNull(page);
                assertEquals(1, page.getTotalItems(), "Должен быть найден только один товар");
                ItemDto dto = page.getItemList().get(0);
                assertEquals(entity1.getName(), dto.getName());
            })
            .verifyComplete();
    }
}