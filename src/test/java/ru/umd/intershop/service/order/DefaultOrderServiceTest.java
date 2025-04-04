package ru.umd.intershop.service.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.umd.intershop.common.constant.CartItemAction;
import ru.umd.intershop.common.constant.OrderStatusEnum;
import ru.umd.intershop.data.entity.*;
import ru.umd.intershop.data.repository.*;
import ru.umd.intershop.service.dto.OrderDto;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class DefaultOrderServiceTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderService orderService; // Это DefaultOrderService, реализованный в реактивном стиле

    @BeforeEach
    public void setUp() {
        // Очистка таблиц перед каждым тестом
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
        itemRepository.deleteAll().block();
    }

    @Test
    public void testFindByIdFound() {
        // Создаем заказ без товаров (OrderEntity теперь не содержит список товаров)
        OrderEntity order = OrderEntity.builder()
            .status(OrderStatusEnum.COMPLETED.name())
            .totalPrice(new BigDecimal("50.00"))
            .build();
        order = orderRepository.save(order).block();

        Mono<OrderDto> result = orderService.findById(order.getId());

        OrderEntity finalOrder = order;
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertEquals(finalOrder.getId(), dto.getId());
                assertEquals(finalOrder.getTotalPrice(), dto.getTotalPrice());
                // В OrderDto список товаров формируется через OrderItemEntity, ожидаем, что он пустой
                assertTrue(dto.getItems().isEmpty(), "Список товаров должен быть пустым");
            })
            .verifyComplete();
    }

    @Test
    public void testFindAllCompleted() {
        OrderEntity order1 = OrderEntity.builder()
            .status(OrderStatusEnum.COMPLETED.name())
            .totalPrice(new BigDecimal("100.00"))
            .build();
        OrderEntity order2 = OrderEntity.builder()
            .status(OrderStatusEnum.COMPLETED.name())
            .totalPrice(new BigDecimal("200.00"))
            .build();
        OrderEntity order3 = OrderEntity.builder()
            .status(OrderStatusEnum.NEW.name())
            .totalPrice(new BigDecimal("300.00"))
            .build();
        orderRepository.saveAll(Arrays.asList(order1, order2, order3))
            .collectList().block();

        Flux<OrderDto> completedOrders = orderService.findAllCompleted();

        StepVerifier.create(completedOrders.collectList())
            .assertNext(list -> {
                assertEquals(2, list.size(), "Должно быть 2 завершённых заказа");
                list.forEach(dto -> assertEquals(OrderStatusEnum.COMPLETED, dto.getStatus()));
            })
            .verifyComplete();
    }

    @Test
    public void testGetCartWhenNotExists() {
        // Если заказ с статусом NEW отсутствует, getCart должен создать новый
        Mono<OrderDto> cartMono = orderService.getCart();

        StepVerifier.create(cartMono)
            .assertNext(cart -> {
                assertNotNull(cart, "Корзина не должна быть null");
                assertEquals(OrderStatusEnum.NEW, cart.getStatus(), "Новый заказ должен иметь статус NEW");
                assertEquals(BigDecimal.ZERO, cart.getTotalPrice(), "Новый заказ должен иметь totalPrice = 0");
                assertTrue(cart.getItems().isEmpty(), "В новом заказе не должно быть товаров");
            })
            .verifyComplete();
    }

    @Test
    public void testUpdateItemCount() {
        // Создаем тестовый товар
        ItemEntity item = ItemEntity.builder()
            .name("Test Item")
            .description("Test Description")
            .price(new BigDecimal("10.00"))
            .imageFileName("test.jpg")
            .isActive(true)
            .build();
        item = itemRepository.save(item).block();

        // Вызов PLUS – товар должен добавиться в корзину через создание OrderItemEntity
        orderService.updateItemCount(item.getId(), CartItemAction.PLUS).block();

        // Получаем корзину (заказ со статусом NEW)
        OrderEntity cart = orderRepository.findLastByStatus(OrderStatusEnum.NEW.name())
            .blockOptional()
            .orElseThrow(() -> new RuntimeException("Корзина не найдена"));
        // Извлекаем OrderItemEntity по orderId
        List<OrderItemEntity> orderItems = orderItemRepository.findAllByOrderIdOrderByIdDesc(cart.getId())
            .collectList().block();
        assertEquals(1, orderItems.size(), "В корзине должен быть один товар");
        OrderItemEntity orderItem = orderItems.get(0);
        assertEquals(1, orderItem.getCount(), "Количество должно быть 1 после первого PLUS");

        // Вызов MINUS – количество должно уменьшиться до 0 (но OrderItemEntity остается)
        orderService.updateItemCount(item.getId(), CartItemAction.MINUS).block();
        cart = orderRepository.findLastByStatus(OrderStatusEnum.NEW.name())
            .blockOptional()
            .orElseThrow();
        orderItems = orderItemRepository.findAllByOrderIdOrderByIdDesc(cart.getId())
            .collectList().block();
        assertFalse(orderItems.isEmpty(), "OrderItem должен присутствовать");
        orderItem = orderItems.get(0);
        assertEquals(0, orderItem.getCount(), "Количество должно стать 0 после MINUS");

        // Вызов DELETE – OrderItemEntity должен быть удален
        orderService.updateItemCount(item.getId(), CartItemAction.DELETE).block();
        cart = orderRepository.findLastByStatus(OrderStatusEnum.NEW.name())
            .blockOptional()
            .orElseThrow();
        orderItems = orderItemRepository.findAllByOrderIdOrderByIdDesc(cart.getId())
            .collectList().block();
        assertTrue(orderItems.isEmpty(), "После DELETE корзина должна быть пуста");
    }

    @Test
    public void testProcessCart() {
        // Создаем товар и добавляем его в корзину через updateItemCount
        ItemEntity item = ItemEntity.builder()
            .name("Test Item")
            .description("Test Desc")
            .price(new BigDecimal("10.00"))
            .imageFileName("test.jpg")
            .isActive(true)
            .build();
        item = itemRepository.save(item).block();

        // Добавляем товар дважды (каждый вызов PLUS увеличивает количество на 1)
        orderService.updateItemCount(item.getId(), CartItemAction.PLUS).block();
        orderService.updateItemCount(item.getId(), CartItemAction.PLUS).block();

        // Обработка корзины: вычисляется totalPrice и статус меняется на COMPLETED
        Mono<Long> processedOrderIdMono = orderService.processCart();
        Long processedOrderId = processedOrderIdMono.block();

        OrderEntity processedOrder = orderRepository.findById(processedOrderId)
            .blockOptional()
            .orElseThrow(() -> new RuntimeException("Обработанный заказ не найден"));
        assertEquals(OrderStatusEnum.COMPLETED.name(), processedOrder.getStatus(), "Статус заказа должен быть COMPLETED");
        // Ожидаем totalPrice = 10.00 * 2 = 20.00
        assertEquals(new BigDecimal("20.00"), processedOrder.getTotalPrice());
    }
}
