package com.familyfinance.family.controller;

import com.familyfinance.family.dto.CreateInvitationRequest;
import com.familyfinance.family.dto.FamilyMemberResponse;
import com.familyfinance.family.dto.InvitationResponse;
import com.familyfinance.family.service.FamilyInvitationService;
import com.familyfinance.family.support.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/families")
public class FamilyInvitationController {

    private final FamilyInvitationService invitationService;
    private final CurrentUserResolver currentUserResolver;

    public FamilyInvitationController(
            FamilyInvitationService invitationService, CurrentUserResolver currentUserResolver) {
        this.invitationService = invitationService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/{familyId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse invite(
            @PathVariable UUID familyId,
            @Valid @RequestBody CreateInvitationRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return invitationService.invite(familyId, request, userId);
    }

    @PostMapping("/invitations/{token}/accept")
    public FamilyMemberResponse acceptInvitation(@PathVariable("token") String token, HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return invitationService.acceptInvitation(token, userId);
    }

    @PostMapping("/invitations/{token}/reject")
    public InvitationResponse rejectInvitation(@PathVariable("token") String token, HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return invitationService.rejectInvitation(token, userId);
    }
}