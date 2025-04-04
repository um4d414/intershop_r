package ru.umd.intershop.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.umd.intershop.service.dto.*;
import ru.umd.intershop.service.order.OrderService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {OrderController.class})
public class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderService orderService;

    // Тест для GET /orders, который возвращает список завершённых заказов
    @Test
    public void testOrders() {
        // Создаем тестовый ItemDto
        ItemDto itemDto = ItemDto.builder()
            .id(1L)
            .name("Test Item")
            .description("Test Description")
            .price(new BigDecimal("10.00"))
            .imageFileName("test.jpg")
            .build();

        // Создаем тестовый OrderItemDto
        OrderItemDto orderItemDto = OrderItemDto.builder()
            .item(itemDto)
            .count(2)
            .build();

        // Создаем тестовый OrderDto
        OrderDto orderDto = OrderDto.builder()
            .id(100L)
            .totalPrice(new BigDecimal("20.00"))
            .items(List.of(orderItemDto))
            .build();

        // Мокаем метод, возвращающий Flux<OrderDto>
        when(orderService.findAllCompleted()).thenReturn(Flux.just(orderDto));

        webTestClient.get()
            .uri("/orders")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                // Проверяем, что в HTML-ответе присутствуют ключевые данные заказа
                assertTrue(body.contains("100"), "Ответ должен содержать id заказа 100");
                assertTrue(body.contains("20.00"), "Ответ должен содержать сумму заказа 20.00");
            });
    }

    // Тест для GET /orders/{id}
    @Test
    public void testOrder() {
        // Создаем тестовый ItemDto
        ItemDto itemDto = ItemDto.builder()
            .id(1L)
            .name("Test Item")
            .description("Test Description")
            .price(new BigDecimal("10.00"))
            .imageFileName("test.jpg")
            .build();

        // Создаем тестовый OrderItemDto
        OrderItemDto orderItemDto = OrderItemDto.builder()
            .item(itemDto)
            .count(2)
            .build();

        // Создаем тестовый OrderDto
        OrderDto orderDto = OrderDto.builder()
            .id(100L)
            .totalPrice(new BigDecimal("20.00"))
            .items(List.of(orderItemDto))
            .build();

        // Мокаем метод, возвращающий Mono<OrderDto>
        when(orderService.findById(100L)).thenReturn(Mono.just(orderDto));

        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/orders/{id}")
                .queryParam("isNew", "true")
                .build(100L))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("100"), "Ответ должен содержать id заказа 100");
                assertTrue(body.contains("20.00"), "Ответ должен содержать сумму заказа 20.00");
                // Проверяем наличие поздравительного сообщения, которое отображается, если newOrder == true
                assertTrue(body.contains("Поздравляем! Успешная покупка!"), "Ответ должен содержать флаг newOrder");
            });
    }

    // Тест для GET /cart/items
    @Test
    public void testCart() {
        // Создаем тестовый ItemDto
        ItemDto itemDto = ItemDto.builder()
            .id(1L)
            .name("Test Item")
            .description("Test Description")
            .price(new BigDecimal("10.00"))
            .imageFileName("test.jpg")
            .build();

        // Создаем тестовый OrderItemDto
        OrderItemDto orderItemDto = OrderItemDto.builder()
            .item(itemDto)
            .count(2)
            .build();

        // Создаем тестовый OrderDto, который представляет корзину
        OrderDto cartDto = OrderDto.builder()
            .id(200L)
            .totalPrice(new BigDecimal("20.00"))
            .items(List.of(orderItemDto))
            .build();

        when(orderService.getCart()).thenReturn(Mono.just(cartDto));

        webTestClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                // Проверяем, что HTML содержит информацию о товаре в корзине
                assertTrue(body.contains("Test Item"), "Ответ должен содержать имя товара 'Test Item'");
                assertTrue(body.contains("20.00"), "Ответ должен содержать сумму заказа 20.00");
            });
    }

    // Тест для POST /buy
    @Test
    public void testProcessOrder() {
        when(orderService.processCart()).thenReturn(Mono.just(300L));

        webTestClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().valueEquals("Location", "/orders/300?isNew=true");
    }
}
