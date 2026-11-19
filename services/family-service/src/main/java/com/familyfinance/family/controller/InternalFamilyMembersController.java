package com.familyfinance.family.controller;

import com.familyfinance.family.dto.InternalFamilyMemberResponse;
import com.familyfinance.family.service.FamilyMemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/families/{familyId}/members")
public class InternalFamilyMembersController {

    private final FamilyMemberService memberService;

    public InternalFamilyMembersController(FamilyMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public List<InternalFamilyMemberResponse> listActive(@PathVariable UUID familyId) {
        return memberService.listActiveMembersInternal(familyId);
    }
}
