package ru.umd.intershop.shop.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.umd.intershop.shop.service.order.OrderService;
import ru.umd.intershop.shop.web.model.ItemModel;
import ru.umd.intershop.shop.web.model.OrderModel;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

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
            .map(cart -> {
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
                return Rendering.view("cart")
                    .modelAttribute("items", items)
                    .modelAttribute("empty", items.isEmpty())
                    .modelAttribute("total", total)
                    .build();
            });
    }

    @PostMapping("/buy")
    public Mono<Rendering> processOrder() {
        return orderService.processCart()
            .map(orderId -> Rendering.redirectTo("/orders/" + orderId + "?isNew=true").build());
    }
}
