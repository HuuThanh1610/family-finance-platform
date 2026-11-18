package com.familyfinance.family.controller;

import com.familyfinance.family.dto.CreateFamilyRequest;
import com.familyfinance.family.dto.FamilyResponse;
import com.familyfinance.family.service.FamilyService;
import com.familyfinance.family.support.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/families")
public class FamilyController {

    private final FamilyService familyService;
    private final CurrentUserResolver currentUserResolver;

    public FamilyController(FamilyService familyService, CurrentUserResolver currentUserResolver) {
        this.familyService = familyService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyResponse create(@Valid @RequestBody CreateFamilyRequest request, HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return familyService.create(request, userId);
    }

    @GetMapping
    public List<FamilyResponse> list(HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return familyService.listForUser(userId);
    }

    @GetMapping("/{id}")
    public FamilyResponse get(@PathVariable UUID id, HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return familyService.findById(id, userId);
    }
}