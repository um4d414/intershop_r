package ru.umd.intershop.shop.web.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.umd.intershop.shop.service.admin.dto.ItemForm;
import ru.umd.intershop.shop.service.admin.item.ItemAdminService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ItemAdminController {
    private final ItemAdminService itemAdminService;

    @GetMapping("/items/add")
    public String showAddItemForm(Model model) {
        model.addAttribute("itemForm", new ItemForm());

        return "admin-item-add";
    }

    @PostMapping("/items/add")
    public Mono<Rendering> addItem(@ModelAttribute("itemForm") ItemForm itemForm) {
        return itemAdminService.createItem(itemForm)
            .then(Mono.just(Rendering.redirectTo("/items/add").build()))
            .onErrorResume(e -> {
                log.error("Ошибка при создании товара: {}", e.getMessage(), e);
                return null;
            });
    }
}
