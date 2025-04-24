package ru.umd.intershop.shop.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.umd.intershop.client.api.PaymentsApi;
import ru.umd.intershop.client.model.BalanceResponse;
import ru.umd.intershop.client.model.PaymentRequest;
import ru.umd.intershop.client.model.PaymentResponse;
import ru.umd.intershop.shop.service.dto.*;
import ru.umd.intershop.shop.service.order.OrderService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {OrderController.class})
public class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderService orderService;

    @MockBean
    private PaymentsApi paymentsApi;

    // Вспомогательный метод для создания тестового заказа
    private OrderDto createTestOrderDto(Long id, BigDecimal price) {
        ItemDto itemDto = ItemDto.builder()
            .id(1L)
            .name("Test Item")
            .description("Test Description")
            .price(price.divide(BigDecimal.valueOf(2)))
            .imageFileName("test.jpg")
            .build();

        OrderItemDto orderItemDto = OrderItemDto.builder()
            .item(itemDto)
            .count(2)
            .build();

        return OrderDto.builder()
            .id(id)
            .totalPrice(price)
            .items(List.of(orderItemDto))
            .build();
    }

    // Тест для GET /orders, который возвращает список завершённых заказов
    @Test
    public void testOrders() {
        OrderDto orderDto = createTestOrderDto(100L, new BigDecimal("20.00"));

        when(orderService.findAllCompleted()).thenReturn(Flux.just(orderDto));

        webTestClient.get()
            .uri("/orders")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("100"), "Ответ должен содержать id заказа 100");
                assertTrue(body.contains("20.00"), "Ответ должен содержать сумму заказа 20.00");
            });
    }

    // Тест для GET /orders/{id}
    @Test
    public void testOrder() {
        OrderDto orderDto = createTestOrderDto(100L, new BigDecimal("20.00"));

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
                assertTrue(body.contains("Поздравляем! Успешная покупка!"), "Ответ должен содержать флаг newOrder");
            });
    }

    // Тест для GET /cart/items с доступным сервисом платежей и достаточными средствами
    @Test
    public void testCartWithSufficientFunds() {
        OrderDto cartDto = createTestOrderDto(200L, new BigDecimal("20.00"));
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(50.00); // Достаточно средств

        when(orderService.getCart()).thenReturn(Mono.just(cartDto));
        when(paymentsApi.getBalance()).thenReturn(Mono.just(balanceResponse));

        webTestClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("Test Item"), "Ответ должен содержать имя товара 'Test Item'");
                assertTrue(body.contains("20.00"), "Ответ должен содержать сумму заказа 20.00");
                assertTrue(body.contains("50.0"), "Ответ должен содержать баланс счета");

                // Проверяем наличие кнопки покупки
                assertTrue(body.contains("<button>Купить</button>"), "Должна отображаться кнопка покупки");

                // Проверяем отсутствие сообщения о недостатке средств
                // Это должно проверять отсутствие блока с сообщением о недостатке средств
                assertFalse(body.contains("Недостаточно средств на счету для оформления заказа!"),
                            "Не должно быть сообщения о недостатке средств");
            });
    }

    // Тест для GET /cart/items с доступным сервисом платежей, но недостаточными средствами
    @Test
    public void testCartWithInsufficientFunds() {
        OrderDto cartDto = createTestOrderDto(200L, new BigDecimal("60.00"));
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(50.00); // Недостаточно средств

        when(orderService.getCart()).thenReturn(Mono.just(cartDto));
        when(paymentsApi.getBalance()).thenReturn(Mono.just(balanceResponse));

        webTestClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("Test Item"), "Ответ должен содержать имя товара 'Test Item'");
                assertTrue(body.contains("60.00"), "Ответ должен содержать сумму заказа 60.00");
                assertTrue(body.contains("50.0"), "Ответ должен содержать баланс счета");
                assertTrue(!body.contains("<button>Купить</button>"), "Не должна отображаться кнопка покупки");
                assertTrue(body.contains("Недостаточно средств"), "Должно быть сообщение о недостатке средств");
            });
    }

    // Тест для GET /cart/items с недоступным сервисом платежей
    @Test
    public void testCartWithUnavailablePaymentService() {
        OrderDto cartDto = createTestOrderDto(200L, new BigDecimal("20.00"));

        when(orderService.getCart()).thenReturn(Mono.just(cartDto));
        when(paymentsApi.getBalance()).thenReturn(Mono.error(new RuntimeException("Сервис недоступен")));

        webTestClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("Test Item"), "Ответ должен содержать имя товара 'Test Item'");
                assertTrue(body.contains("20.00"), "Ответ должен содержать сумму заказа 20.00");
                assertTrue(!body.contains("<button>Купить</button>"), "Не должна отображаться кнопка покупки");
                assertTrue(body.contains("Сервис платежей недоступен"), "Должно быть сообщение о недоступности сервиса");
            });
    }

    // Тест для POST /buy с успешным платежом
    @Test
    public void testProcessOrderSuccess() {
        OrderDto cartDto = createTestOrderDto(200L, new BigDecimal("20.00"));
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setSuccess(true);
        paymentResponse.setRemainingBalance(30.00);

        when(orderService.getCart()).thenReturn(Mono.just(cartDto));
        when(paymentsApi.makePayment(any(PaymentRequest.class))).thenReturn(Mono.just(paymentResponse));
        when(orderService.processCart()).thenReturn(Mono.just(300L));

        webTestClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().valueEquals("Location", "/orders/300?isNew=true");
    }

    // Тест для POST /buy с неуспешным платежом (отказано сервисом)
    @Test
    public void testProcessOrderFailure() {
        OrderDto cartDto = createTestOrderDto(200L, new BigDecimal("20.00"));
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setSuccess(false);
        paymentResponse.setRemainingBalance(10.00);

        when(orderService.getCart()).thenReturn(Mono.just(cartDto));
        when(paymentsApi.makePayment(any(PaymentRequest.class))).thenReturn(Mono.just(paymentResponse));

        webTestClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().valueEquals("Location", "/cart/items?paymentError=true");
    }

    // Тест для POST /buy с ошибкой "недостаточно средств" (HTTP 400)
    @Test
    public void testProcessOrderInsufficientFunds() {
        OrderDto cartDto = createTestOrderDto(200L, new BigDecimal("20.00"));
        WebClientResponseException badRequestException = WebClientResponseException.create(
            400, "Bad Request", null, null, null);

        when(orderService.getCart()).thenReturn(Mono.just(cartDto));
        when(paymentsApi.makePayment(any(PaymentRequest.class))).thenReturn(Mono.error(badRequestException));

        webTestClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().valueEquals("Location", "/cart/items?paymentError=true");
    }

    // Тест для POST /buy с недоступным сервисом платежей
    @Test
    public void testProcessOrderServiceUnavailable() {
        OrderDto cartDto = createTestOrderDto(200L, new BigDecimal("20.00"));

        when(orderService.getCart()).thenReturn(Mono.just(cartDto));
        when(paymentsApi.makePayment(any(PaymentRequest.class))).thenReturn(Mono.error(new RuntimeException("Сервис недоступен")));

        webTestClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().valueEquals("Location", "/cart/items?serviceError=true");
    }
}