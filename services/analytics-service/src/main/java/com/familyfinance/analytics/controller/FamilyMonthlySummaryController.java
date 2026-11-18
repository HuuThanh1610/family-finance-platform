package com.familyfinance.analytics.controller;

import com.familyfinance.analytics.dto.FamilyMonthlySummaryResponse;
import com.familyfinance.analytics.service.FamilyMonthlySummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/family-monthly-summaries")
public class FamilyMonthlySummaryController {

    private final FamilyMonthlySummaryService service;

    public FamilyMonthlySummaryController(FamilyMonthlySummaryService service) {
        this.service = service;
    }

    @GetMapping
    public List<FamilyMonthlySummaryResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public FamilyMonthlySummaryResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }
}
