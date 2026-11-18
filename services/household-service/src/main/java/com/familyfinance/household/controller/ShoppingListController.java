package com.familyfinance.household.controller;

import com.familyfinance.household.dto.ShoppingListResponse;
import com.familyfinance.household.service.ShoppingListService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shopping-lists")
public class ShoppingListController {

    private final ShoppingListService service;

    public ShoppingListController(ShoppingListService service) {
        this.service = service;
    }

    @GetMapping
    public List<ShoppingListResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ShoppingListResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }
}
