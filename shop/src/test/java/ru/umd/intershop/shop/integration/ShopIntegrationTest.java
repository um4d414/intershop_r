package ru.umd.intershop.shop.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.umd.intershop.client.api.PaymentsApi;
import ru.umd.intershop.client.model.BalanceResponse;
import ru.umd.intershop.client.model.PaymentRequest;
import ru.umd.intershop.client.model.PaymentResponse;
import ru.umd.intershop.shop.common.constant.OrderStatusEnum;
import ru.umd.intershop.shop.config.TestcontainersConfiguration;
import ru.umd.intershop.shop.data.entity.ItemEntity;
import ru.umd.intershop.shop.data.entity.OrderEntity;
import ru.umd.intershop.shop.data.repository.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestcontainersConfiguration.class)
public class ShopIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @MockBean
    private PaymentsApi paymentsApi;

    @BeforeEach
    public void setup() {
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
        itemRepository.deleteAll().block();

        // Очищаем состояние моков перед каждым тестом
        reset(paymentsApi);

        // Настройка моков для сервиса платежей
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(1000.00);
        when(paymentsApi.getBalance()).thenReturn(Mono.just(balanceResponse));

        // Мокируем ответ от makePayment с любым запросом
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setSuccess(true);
        paymentResponse.setRemainingBalance(900.00);
        when(paymentsApi.makePayment(any(PaymentRequest.class)))
            .thenReturn(Mono.just(paymentResponse));
    }

    /**
     * Полная цепочка покупки товара
     */
    @Test
    public void testFullPurchaseFlow() {
        // Шаг 1. Создаём и сохраняем тестовый товар.
        ItemEntity item = ItemEntity.builder()
            .name("Test Item")
            .description("Description for Test Item")
            .price(new BigDecimal("100.00"))
            .imageFileName("test.jpg")
            .isActive(true)
            .build();
        item = itemRepository.save(item).block();

        // Шаг 2. Загружаем страницу товаров
        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/main/items")
                .queryParam("pageSize", "10")
                .queryParam("pageNumber", "0")
                .queryParam("sort", "NO")
                .queryParam("search", "Test")
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("Страница:"), "Страница должна содержать информацию о пагинации");
                assertTrue(
                    body.contains("items") || body.contains("В корзину"),
                    "Страница должна содержать список товаров"
                );
            });

        // Шаг 3. Добавляем товар в корзину
        webTestClient.post()
            .uri("/items/{id}?action=PLUS", item.getId())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("action", "PLUS"))
            .exchange()
            .expectStatus().is3xxRedirection();

        // Шаг 4. Проверяем страницу корзины.
        webTestClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("100.00"), "Корзина должна отображать стоимость 100.00");
                assertTrue(body.contains("Test Item"), "Корзина должна содержать добавленный товар");
                assertTrue(body.contains("1000.0"), "Корзина должна отображать баланс счета");
                assertTrue(body.contains("<button>Купить</button>"), "Должна отображаться кнопка покупки");
            });

        // Сбрасываем счетчики вызовов моков перед оформлением заказа
        reset(paymentsApi);

        // Настраиваем моки заново после сброса
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(1000.00);
        when(paymentsApi.getBalance()).thenReturn(Mono.just(balanceResponse));

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setSuccess(true);
        paymentResponse.setRemainingBalance(900.00);
        when(paymentsApi.makePayment(any(PaymentRequest.class)))
            .thenReturn(Mono.just(paymentResponse));

        // Шаг 5. Оформляем заказ.
        webTestClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().valueMatches("Location", "/orders/.*\\?isNew=true");

        // Захватываем аргумент, который был передан в метод makePayment
        ArgumentCaptor<PaymentRequest> paymentCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentsApi).makePayment(paymentCaptor.capture());

        // Проверяем, что сумма в запросе близка к 100.00 (с учетом возможной погрешности)
        PaymentRequest capturedRequest = paymentCaptor.getValue();
        assertNotNull(capturedRequest, "Запрос платежа не должен быть null");
        assertTrue(Math.abs(capturedRequest.getAmount() - 100.0) < 0.01,
                   "Сумма платежа должна быть около 100.00, получено: " + capturedRequest.getAmount());

        // Шаг 6. Проверяем, что заказ обработан корректно.
        OrderEntity processedOrder = orderRepository.findAll()
            .filter(order -> OrderStatusEnum.COMPLETED.name().equals(order.getStatus()))
            .blockLast();
        assertNotNull(processedOrder, "Processed order not found");
        assertEquals(new BigDecimal("100.00"), processedOrder.getTotalPrice(), "TotalPrice должен быть 100.00");
    }

    /**
     * Сценарий с добавлением нескольких товаров, удалением одного и оформлением заказа.
     */
    @Test
    public void testMultipleItemsAndDeleteFlow() {
        // Шаг 1. Создаём два товара.
        ItemEntity item1 = ItemEntity.builder()
            .name("Item 1")
            .description("Desc 1")
            .price(new BigDecimal("50.00"))
            .imageFileName("item1.jpg")
            .isActive(true)
            .build();
        ItemEntity item2 = ItemEntity.builder()
            .name("Item 2")
            .description("Desc 2")
            .price(new BigDecimal("150.00"))
            .imageFileName("item2.jpg")
            .isActive(true)
            .build();
        item1 = itemRepository.save(item1).block();
        item2 = itemRepository.save(item2).block();

        // Шаг 2. Добавляем товары в корзину
        webTestClient.post()
            .uri("/items/{id}", item1.getId())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("action", "PLUS"))
            .exchange()
            .expectStatus().is3xxRedirection();
        webTestClient.post()
            .uri("/items/{id}", item1.getId())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("action", "PLUS"))
            .exchange()
            .expectStatus().is3xxRedirection();
        webTestClient.post()
            .uri("/items/{id}", item2.getId())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("action", "PLUS"))
            .exchange()
            .expectStatus().is3xxRedirection();

        // Шаг 3. Удаляем товар item1.
        webTestClient.post()
            .uri("/items/{id}", item1.getId())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("action", "DELETE"))
            .exchange()
            .expectStatus().is3xxRedirection();

        // Шаг 4. Проверяем корзину
        webTestClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("150.00"), "Корзина должна отображать стоимость 150.00");
                assertFalse(body.contains("Item 1"), "Корзина не должна содержать Item 1");
                assertTrue(body.contains("1000.0"), "Корзина должна отображать баланс счета");
                assertTrue(body.contains("<button>Купить</button>"), "Должна отображаться кнопка покупки");
            });

        // Сбрасываем счетчики вызовов моков
        reset(paymentsApi);

        // Настраиваем моки заново
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(1000.00);
        when(paymentsApi.getBalance()).thenReturn(Mono.just(balanceResponse));

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setSuccess(true);
        paymentResponse.setRemainingBalance(850.00);
        when(paymentsApi.makePayment(any(PaymentRequest.class)))
            .thenReturn(Mono.just(paymentResponse));

        // Шаг 5. Оформляем заказ.
        webTestClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection();

        // Захватываем аргумент, который был передан в метод makePayment
        ArgumentCaptor<PaymentRequest> paymentCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentsApi).makePayment(paymentCaptor.capture());

        // Проверяем сумму в запросе
        PaymentRequest capturedRequest = paymentCaptor.getValue();
        assertNotNull(capturedRequest, "Запрос платежа не должен быть null");
        assertTrue(Math.abs(capturedRequest.getAmount() - 150.0) < 0.01,
                   "Сумма платежа должна быть около 150.00, получено: " + capturedRequest.getAmount());

        // Шаг 6. Проверяем, что заказ обработан корректно.
        OrderEntity processedOrder = orderRepository.findAll()
            .filter(order -> OrderStatusEnum.COMPLETED.name().equals(order.getStatus()))
            .blockLast();
        assertNotNull(processedOrder, "Processed order not found");
        assertEquals(new BigDecimal("150.00"), processedOrder.getTotalPrice(), "TotalPrice должен быть 150.00");
    }

    /**
     * Сценарий с недостаточными средствами на счету.
     */
    @Test
    public void testInsufficientFundsFlow() {
        // Настраиваем баланс с меньшей суммой для этого теста
        BalanceResponse lowBalanceResponse = new BalanceResponse();
        lowBalanceResponse.setBalance(500.00);
        when(paymentsApi.getBalance()).thenReturn(Mono.just(lowBalanceResponse));

        // Создаём дорогой товар
        ItemEntity expensiveItem = ItemEntity.builder()
            .name("Expensive Item")
            .description("Very expensive item")
            .price(new BigDecimal("2000.00"))
            .imageFileName("expensive.jpg")
            .isActive(true)
            .build();
        expensiveItem = itemRepository.save(expensiveItem).block();

        // Добавляем товар в корзину
        webTestClient.post()
            .uri("/items/{id}?action=PLUS", expensiveItem.getId())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("action", "PLUS"))
            .exchange()
            .expectStatus().is3xxRedirection();

        // Проверяем страницу корзины
        webTestClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("2000.00"), "Корзина должна отображать стоимость 2000.00");
                assertTrue(body.contains("500.0"), "Корзина должна отображать баланс счета");
                assertTrue(body.contains("Недостаточно средств"), "Должно быть сообщение о недостатке средств");
                assertFalse(body.contains("<button>Купить</button>"), "Не должна отображаться кнопка покупки");
            });
    }

    /**
     * Сценарий с недоступным сервисом платежей.
     */
    @Test
    public void testUnavailablePaymentServiceFlow() {
        // Настраиваем мок на возврат ошибки
        when(paymentsApi.getBalance()).thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        // Создаём тестовый товар
        ItemEntity item = ItemEntity.builder()
            .name("Test Item")
            .description("Test Item")
            .price(new BigDecimal("100.00"))
            .imageFileName("test.jpg")
            .isActive(true)
            .build();
        item = itemRepository.save(item).block();

        // Добавляем товар в корзину
        webTestClient.post()
            .uri("/items/{id}?action=PLUS", item.getId())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("action", "PLUS"))
            .exchange()
            .expectStatus().is3xxRedirection();

        // Проверяем страницу корзины
        webTestClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("100.00"), "Корзина должна отображать стоимость 100.00");
                assertTrue(body.contains("Сервис платежей недоступен"), "Должно быть сообщение о недоступности сервиса");
                assertFalse(body.contains("<button>Купить</button>"), "Не должна отображаться кнопка покупки");
            });
    }

    /**
     * Сценарий с ошибкой платежа при покупке.
     */
    @Test
    public void testPaymentFailureFlow() {
        // Создаём тестовый товар
        ItemEntity item = ItemEntity.builder()
            .name("Test Item")
            .description("Test Item")
            .price(new BigDecimal("100.00"))
            .imageFileName("test.jpg")
            .isActive(true)
            .build();
        item = itemRepository.save(item).block();

        // Добавляем товар в корзину
        webTestClient.post()
            .uri("/items/{id}?action=PLUS", item.getId())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("action", "PLUS"))
            .exchange()
            .expectStatus().is3xxRedirection();

        // Настраиваем мок на возврат неудачного платежа
        PaymentResponse failedPaymentResponse = new PaymentResponse();
        failedPaymentResponse.setSuccess(false);
        failedPaymentResponse.setRemainingBalance(1000.00);
        when(paymentsApi.makePayment(any(PaymentRequest.class)))
            .thenReturn(Mono.just(failedPaymentResponse));

        // Пытаемся оформить заказ
        webTestClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().valueEquals("Location", "/cart/items?paymentError=true");

        // Проверяем, что заказ не был создан
        long completedOrderCount = orderRepository.findAll()
            .filter(order -> OrderStatusEnum.COMPLETED.name().equals(order.getStatus()))
            .count()
            .block();
        assertEquals(0, completedOrderCount, "Не должно быть завершенных заказов");
    }

    /**
     * Сценарий с ошибкой HTTP 400 (недостаточно средств) при покупке.
     */
    @Test
    public void testPaymentBadRequestFlow() {
        // Создаём тестовый товар
        ItemEntity item = ItemEntity.builder()
            .name("Test Item")
            .description("Test Item")
            .price(new BigDecimal("100.00"))
            .imageFileName("test.jpg")
            .isActive(true)
            .build();
        item = itemRepository.save(item).block();

        // Добавляем товар в корзину
        webTestClient.post()
            .uri("/items/{id}?action=PLUS", item.getId())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("action", "PLUS"))
            .exchange()
            .expectStatus().is3xxRedirection();

        // Настраиваем мок на возврат ошибки 400 (недостаточно средств)
        WebClientResponseException badRequestException = WebClientResponseException.create(
            400, "Bad Request", null, null, null);
        when(paymentsApi.makePayment(any(PaymentRequest.class)))
            .thenReturn(Mono.error(badRequestException));

        // Пытаемся оформить заказ
        webTestClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader().valueEquals("Location", "/cart/items?paymentError=true");

        // Проверяем, что заказ не был создан
        long completedOrderCount = orderRepository.findAll()
            .filter(order -> OrderStatusEnum.COMPLETED.name().equals(order.getStatus()))
            .count()
            .block();
        assertEquals(0, completedOrderCount, "Не должно быть завершенных заказов");
    }
}