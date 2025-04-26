package ru.umd.intershop.shop.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.umd.intershop.client.api.PaymentsApi;
import ru.umd.intershop.client.model.PaymentRequest;
import ru.umd.intershop.shop.service.order.OrderService;
import ru.umd.intershop.shop.web.model.ItemModel;
import ru.umd.intershop.shop.web.model.OrderModel;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final OrderService orderService;

    private final PaymentsApi paymentsApi;

    @GetMapping("/orders")
    public Mono<Rendering> orders() {
        return orderService.findAllCompleted()
            .collectList()
            .map(orderDtoList -> {
                List<OrderModel> orderModels = orderDtoList.stream()
                    .map(
                        orderDto ->
                            OrderModel.builder()
                                .id(orderDto.getId())
                                .items(orderDto.getItems().stream()
                                           .map(orderItemDto -> ItemModel.builder()
                                               .title(orderItemDto.getItem().getName())
                                               .count(orderItemDto.getCount())
                                               .price(orderItemDto.getItem().getPrice())
                                               .build())
                                           .toList())
                                .total(orderDto.getTotalPrice().toPlainString())
                                .build()
                    )
                    .toList();
                return Rendering.view("orders")
                    .modelAttribute("orders", orderModels)
                    .build();
            });
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> order(
        @PathVariable("id") Long id,
        @RequestParam(required = false, defaultValue = "false") Boolean isNew
    ) {
        return orderService.findById(id)
            .map(orderDto -> {
                OrderModel orderModel = OrderModel.builder()
                    .id(orderDto.getId())
                    .items(orderDto.getItems().stream()
                               .map(orderItemDto -> ItemModel.builder()
                                   .id(orderItemDto.getItem().getId())
                                   .title(orderItemDto.getItem().getName())
                                   .description(orderItemDto.getItem().getDescription())
                                   .imgPath(orderItemDto.getItem().getImagePath())
                                   .count(orderItemDto.getCount())
                                   .price(orderItemDto.getItem().getPrice())
                                   .build())
                               .toList())
                    .total(orderDto.getTotalPrice().toPlainString())
                    .build();
                return Rendering.view("order")
                    .modelAttribute("order", orderModel)
                    .modelAttribute("newOrder", isNew)
                    .build();
            });
    }

    @GetMapping("/cart/items")
    public Mono<Rendering> cart() {
        return orderService.getCart()
            .flatMap(cart -> {
                List<ItemModel> items = cart.getItems().stream()
                    .map(orderItemDto -> ItemModel.builder()
                        .id(orderItemDto.getItem().getId())
                        .title(orderItemDto.getItem().getName())
                        .description(orderItemDto.getItem().getDescription())
                        .imgPath(orderItemDto.getItem().getImagePath())
                        .count(orderItemDto.getCount())
                        .price(orderItemDto.getItem().getPrice())
                        .build())
                    .toList();
                BigDecimal total = cart.getItems().stream()
                    .map(orderItem -> orderItem.getItem().getPrice()
                        .multiply(BigDecimal.valueOf(orderItem.getCount())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Если корзина пуста, просто отображаем пустую корзину
                if (items.isEmpty()) {
                    return Mono.just(Rendering.view("cart")
                                         .modelAttribute("items", items)
                                         .modelAttribute("empty", true)
                                         .modelAttribute("total", total)
                                         .modelAttribute("serviceAvailable", true)
                                         .build());
                }

                // Пытаемся получить баланс
                return paymentsApi.getBalance()
                    .map(balanceResponse -> {
                        log.atInfo().log("баланс: " + balanceResponse.getBalance());
                        BigDecimal balance = BigDecimal.valueOf(balanceResponse.getBalance());
                        boolean insufficientFunds = balance.compareTo(total) < 0;

                        return Rendering.view("cart")
                            .modelAttribute("items", items)
                            .modelAttribute("empty", false)
                            .modelAttribute("total", total)
                            .modelAttribute("insufficientFunds", insufficientFunds)
                            .modelAttribute("serviceAvailable", true) // Сервис доступен
                            .modelAttribute("balance", balance)
                            .build();
                    })
                    .onErrorResume(ex -> {
                        // Случилась ошибка при получении баланса
                        return Mono.just(Rendering.view("cart")
                                             .modelAttribute("items", items)
                                             .modelAttribute("empty", items.isEmpty())
                                             .modelAttribute("total", total)
                                             .modelAttribute("serviceAvailable", false) // Сервис недоступен
                                             .build());
                    });
            });
    }

    @PostMapping("/buy")
    public Mono<Rendering> processOrder() {
        return orderService.getCart()
            .flatMap(cart -> {
                // Вычисляем общую сумму точно так же, как в методе cart()
                BigDecimal totalAmount = cart.getItems().stream()
                    .map(orderItem -> orderItem.getItem().getPrice()
                        .multiply(BigDecimal.valueOf(orderItem.getCount())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                var paymentRequest = new PaymentRequest();
                paymentRequest.setAmount(totalAmount.doubleValue());

                return paymentsApi.makePayment(paymentRequest)
                    .flatMap(response -> {
                        if (response.getSuccess()) {
                            return orderService.processCart()
                                .map(orderId -> Rendering.redirectTo("/orders/" + orderId + "?isNew=true").build());
                        } else {
                            return Mono.just(Rendering.redirectTo("/cart/items?paymentError=true").build());
                        }
                    })
                    .onErrorResume(ex -> {
                        if (ex instanceof WebClientResponseException.BadRequest) {
                            // Недостаточно средств на счете
                            return Mono.just(Rendering.redirectTo("/cart/items?paymentError=true").build());
                        } else {
                            // Сервис недоступен или другая ошибка
                            return Mono.just(Rendering.redirectTo("/cart/items?serviceError=true").build());
                        }
                    });
            });
    }

}
