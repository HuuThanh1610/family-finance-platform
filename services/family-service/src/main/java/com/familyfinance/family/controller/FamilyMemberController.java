package com.familyfinance.family.controller;

import com.familyfinance.family.dto.FamilyMemberResponse;
import com.familyfinance.family.dto.UpdateMemberRoleRequest;
import com.familyfinance.family.service.FamilyMemberService;
import com.familyfinance.family.support.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/families/{familyId}/members")
public class FamilyMemberController {

    private final FamilyMemberService memberService;
    private final CurrentUserResolver currentUserResolver;

    public FamilyMemberController(FamilyMemberService memberService, CurrentUserResolver currentUserResolver) {
        this.memberService = memberService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public List<FamilyMemberResponse> list(@PathVariable UUID familyId, HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return memberService.listMembers(familyId, userId);
    }

    @PatchMapping("/{memberId}/role")
    public FamilyMemberResponse updateRole(
            @PathVariable UUID familyId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        return memberService.updateRole(familyId, memberId, request, userId);
    }

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @PathVariable UUID familyId, @PathVariable UUID memberId, HttpServletRequest httpRequest) {
        UUID userId = currentUserResolver.requireUserProfileId(httpRequest);
        memberService.removeMember(familyId, memberId, userId);
    }
}