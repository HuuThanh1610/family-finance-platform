package com.familyfinance.family.controller;

import com.familyfinance.family.dto.MemberAuthorizationResponse;
import com.familyfinance.family.service.FamilyAuthorizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/families/{familyId}/members/{userProfileId}")
public class InternalFamilyAuthorizationController {

    private final FamilyAuthorizationService authorizationService;

    public InternalFamilyAuthorizationController(FamilyAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/authorization")
    public MemberAuthorizationResponse authorization(
            @PathVariable UUID familyId, @PathVariable UUID userProfileId) {
        return authorizationService.resolveAuthorization(familyId, userProfileId);
    }
}