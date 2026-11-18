package com.familyfinance.family.controller;

import com.familyfinance.family.dto.FamilyResponse;
import com.familyfinance.family.service.FamilyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/families")
public class FamilyController {

    private final FamilyService service;

    public FamilyController(FamilyService service) {
        this.service = service;
    }

    @GetMapping
    public List<FamilyResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public FamilyResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }
}
