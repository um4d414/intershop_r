package ru.umd.intershop.web.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import ru.umd.intershop.service.admin.dto.ItemForm;
import ru.umd.intershop.service.admin.item.ItemAdminService;

import java.beans.PropertyEditorSupport;

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
    public String addItem(@ModelAttribute("itemForm") ItemForm itemForm) {
        itemAdminService.createItem(itemForm);

        return "redirect:/items/add";
    }
}
