package ru.umd.intershop.web.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.umd.intershop.common.constant.CartItemAction;
import ru.umd.intershop.common.constant.ItemSortingEnum;
import ru.umd.intershop.service.dto.*;
import ru.umd.intershop.service.item.ItemService;
import ru.umd.intershop.service.order.OrderService;
import ru.umd.intershop.web.model.ItemModel;
import ru.umd.intershop.web.model.PagingModel;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ItemController {
    private static final int ITEM_ROW_SIZE = 3;

    private final ItemService itemService;

    private final OrderService orderService;

    @GetMapping("/main/items")
    public Mono<Rendering> mainPage(
        Model model,
        @RequestParam(name = "pageSize", defaultValue = "10") @Max(100) Integer pageSize,
        @RequestParam(name = "pageNumber", defaultValue = "0") @Min(0) Integer pageNumber,
        @RequestParam(name = "sort", defaultValue = "NO") ItemSortingEnum sort,
        @RequestParam(name = "search", required = false) String search
    ) {
        Mono<ItemPageDto> itemsPageMono = itemService.findAllActive(
            Pageable.ofSize(pageSize).withPage(pageNumber),
            sort,
            search
        );

        Mono<OrderDto> cartMono = orderService.getCart();

        return Mono.zip(itemsPageMono, cartMono)
            .map(tuple -> {
                ItemPageDto itemPageDto = tuple.getT1();
                OrderDto cart = tuple.getT2();

                List<ItemModel> itemsDtoList = itemPageDto.getItemList().stream()
                    .map(item -> {
                        Optional<OrderItemDto> orderItemOpt = cart.getItems().stream()
                            .filter(orderItem -> orderItem.getItem().getId().equals(item.getId()))
                            .findFirst();
                        return ItemModel.builder()
                            .id(item.getId())
                            .imgPath(item.getImagePath())
                            .title(item.getName())
                            .description(item.getDescription())
                            .price(item.getPrice())
                            .count(orderItemOpt.map(OrderItemDto::getCount).orElse(0))
                            .build();
                    })
                    .collect(Collectors.toList());

                List<List<ItemModel>> structuredByRowItems = new ArrayList<>();
                for (int i = 0; i < itemsDtoList.size(); i += ITEM_ROW_SIZE) {
                    structuredByRowItems.add(
                        itemsDtoList.subList(i, Math.min(i + ITEM_ROW_SIZE, itemsDtoList.size()))
                    );
                }

                model.addAttribute("paging", PagingModel.of(itemPageDto));
                model.addAttribute("items", structuredByRowItems);

                return Rendering.view("main")
                    .modelAttributes(model.asMap())
                    .build();
            });
    }

    @GetMapping(path = "/items/{id}")
    public Mono<Rendering> itemPage(@PathVariable Long id) {
        return Mono.zip(
                itemService.findById(id)
                    .switchIfEmpty(Mono.error(new RuntimeException("Item not found"))),
                orderService.getCart()
            )
            .map(tuple -> {
                var item = tuple.getT1();
                var cart = tuple.getT2();

                var orderItem = cart.getItems().stream()
                    .filter(it -> it.getItem().getId().equals(item.getId()))
                    .findFirst();

                var itemModel = ItemModel.builder()
                    .id(item.getId())
                    .imgPath("images/" + item.getImageFileName())
                    .title(item.getName())
                    .description(item.getDescription())
                    .price(item.getPrice())
                    .count(orderItem.map(OrderItemDto::getCount).orElse(0))
                    .build();

                return Rendering.view("item")
                    .modelAttribute("item", itemModel)
                    .build();
            });
    }

    @PostMapping(path = "/items/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> updateCartItem(@PathVariable Long id, ServerWebExchange exchange) {
        return exchange.getFormData().flatMap(formData -> {
            if (!formData.containsKey("action")) {
                return Mono.just(Rendering.redirectTo("redirectBack").build());
            }
            CartItemAction action = CartItemAction.valueOf(formData.getFirst("action").toUpperCase());
            return orderService.updateItemCount(id, action)
                .then(Mono.just(Rendering.redirectTo("redirectBack").build()));
        });
    }
}