package ru.umd.intershop.shop.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.umd.intershop.shop.common.constant.CartItemAction;
import ru.umd.intershop.shop.common.constant.ItemSortingEnum;
import ru.umd.intershop.shop.service.dto.*;
import ru.umd.intershop.shop.service.item.ItemService;
import ru.umd.intershop.shop.service.order.OrderService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(ItemController.class)
public class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ItemService itemService;

    @MockBean
    private OrderService orderService;

    @Test
    void testMainPage() {
        // Подготовка тестовых данных для страницы /main/items
        ItemDto item1 = ItemDto.builder()
            .id(1L)
            .name("Item 1")
            .description("Description 1")
            .price(new BigDecimal("10.00"))
            .imageFileName("1.jpg")
            .build();
        ItemDto item2 = ItemDto.builder()
            .id(2L)
            .name("Item 2")
            .description("Description 2")
            .price(new BigDecimal("20.00"))
            .imageFileName("2.jpg")
            .build();

        // Создаем объект ItemPageDto с информацией о товарах и пагинации
        ItemPageDto pageDto = ItemPageDto.builder()
            .itemList(Arrays.asList(item1, item2))
            .totalItems(2)
            .totalPages(1)
            .page(0)
            .pageSize(10)
            .build();

        when(itemService.findAllActive(any(Pageable.class), any(ItemSortingEnum.class), anyString()))
            .thenReturn(Mono.just(pageDto));

        // Корзина без товаров
        OrderDto cartDto = OrderDto.builder()
            .id(100L)
            .totalPrice(BigDecimal.ZERO)
            .items(Collections.emptyList())
            .build();
        when(orderService.getCart()).thenReturn(Mono.just(cartDto));

        webTestClient.get()
            .uri(uriBuilder -> uriBuilder.path("/main/items")
                .queryParam("pageSize", "10")
                .queryParam("pageNumber", "0")
                .queryParam("sort", "NO")
                .queryParam("search", "test")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                // Проверяем наличие ключевых элементов в отрендеренном HTML
                // Например, проверяем наличие информации о пагинации и списка товаров.
                assertTrue(body.contains("Страница:"), "Страница должна содержать информацию о пагинации");
                assertTrue(body.contains("items"), "Страница должна содержать список товаров");
            });
    }

    @Test
    void testItemPage() {
        // Подготовка тестовых данных для страницы товара /items/{id}
        ItemDto itemDto = ItemDto.builder()
            .id(1L)
            .name("Item 1")
            .description("Description 1")
            .price(new BigDecimal("10.00"))
            .imageFileName("1.jpg")
            .build();
        when(itemService.findById(1L)).thenReturn(Mono.just(itemDto));

        // Корзина без товаров
        OrderDto cartDto = OrderDto.builder()
            .id(100L)
            .totalPrice(BigDecimal.ZERO)
            .items(Collections.emptyList())
            .build();
        when(orderService.getCart()).thenReturn(Mono.just(cartDto));

        webTestClient.get().uri("/items/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("Item 1"), "Страница должна содержать информацию о товаре");
            });
    }

    @Test
    void testUpdateCartItem() {
        when(orderService.updateItemCount(eq(1L), any(CartItemAction.class)))
            .thenReturn(Mono.empty());

        webTestClient.post()
            .uri("/items/1")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("action", "PLUS"))
            .header("Referer", "/some-url")
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().valueEquals("Location", "/some-url");
    }
}
