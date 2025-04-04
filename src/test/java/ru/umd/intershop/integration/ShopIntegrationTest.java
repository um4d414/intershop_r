package ru.umd.intershop.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.umd.intershop.common.constant.OrderStatusEnum;
import ru.umd.intershop.data.entity.ItemEntity;
import ru.umd.intershop.data.entity.OrderEntity;
import ru.umd.intershop.data.repository.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class ShopIntegrationTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @BeforeEach
    public void setup() {
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
        itemRepository.deleteAll().block();
    }

    /**
     * Полная цепочка: 1. Добавляем товар в БД. 2. Загружаем страницу с товарами (/main/items). 3. Добавляем товар в
     * корзину (POST /items/{id}?action=PLUS). 4. Проверяем, что в корзине отображается товар. 5. Оформляем заказ (POST
     * /buy). 6. Проверяем, что заказ обработан (статус COMPLETED, totalPrice рассчитан).
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

        // Шаг 2. Загружаем страницу товаров (/main/items).
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
            // Поскольку возвращается Rendering (HTML), можно проверить наличие ключевых слов в теле ответа.
            .expectBody(String.class)
            .value(body -> {
                assertTrue(body.contains("Страница:"), "Страница должна содержать информацию о пагинации");
                assertTrue(body.contains("items") || body.contains("В корзину"), "Страница должна содержать список товаров");
            });

        // Шаг 3. Добавляем товар в корзину (POST /items/{id}?action=PLUS).
        webTestClient.post()
            .uri("/items/{id}?action=PLUS", item.getId())
            .exchange()
            .expectStatus().is3xxRedirection();

        // Шаг 4. Проверяем страницу корзины.
        webTestClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                // Проверяем, что страница корзины содержит цену и, например, имя товара.
                assertTrue(body.contains("100.00"), "Корзина должна отображать стоимость 100.00");
                assertTrue(body.contains("Test Item"), "Корзина должна содержать добавленный товар");
            });

        // Шаг 5. Оформляем заказ.
        webTestClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection()
            // Проверяем, что редирект соответствует шаблону "/orders/{id}?isNew=true".
            .expectHeader().valueMatches("Location", "/orders/.*\\?isNew=true");

        // Шаг 6. Проверяем, что заказ обработан (статус COMPLETED, totalPrice рассчитан).
        // Здесь можно заблокировать реактивный поток для получения результата из репозитория.
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

        // Шаг 2. Добавляем товар item1 дважды и item2 один раз.
        webTestClient.post()
            .uri("/items/{id}?action=PLUS", item1.getId())
            .exchange()
            .expectStatus().is3xxRedirection();
        webTestClient.post()
            .uri("/items/{id}?action=PLUS", item1.getId())
            .exchange()
            .expectStatus().is3xxRedirection();
        webTestClient.post()
            .uri("/items/{id}?action=PLUS", item2.getId())
            .exchange()
            .expectStatus().is3xxRedirection();

        // Шаг 3. Удаляем товар item1.
        webTestClient.post()
            .uri("/items/{id}?action=DELETE", item1.getId())
            .exchange()
            .expectStatus().is3xxRedirection();

        // Шаг 4. Проверяем корзину: ожидается, что остался только item2 с count = 1, total = 150.00.
        webTestClient.get()
            .uri("/cart/items")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> {
                // Проверяем, что страница корзины содержит цену товара item2.
                assertTrue(body.contains("150.00"), "Корзина должна отображать стоимость 150.00");
                // Можно добавить проверку на отсутствие информации о item1.
                assertFalse(body.contains("Item 1"), "Корзина не должна содержать Item 1");
            });

        // Шаг 5. Оформляем заказ.
        webTestClient.post()
            .uri("/buy")
            .exchange()
            .expectStatus().is3xxRedirection();

        // Шаг 6. Проверяем, что заказ обработан корректно.
        OrderEntity processedOrder = orderRepository.findAll()
            .filter(order -> OrderStatusEnum.COMPLETED.name().equals(order.getStatus()))
            .blockLast();
        assertNotNull(processedOrder, "Processed order not found");
        assertEquals(new BigDecimal("150.00"), processedOrder.getTotalPrice(), "TotalPrice должен быть 150.00");
    }
}


