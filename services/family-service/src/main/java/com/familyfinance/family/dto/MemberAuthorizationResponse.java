package com.familyfinance.family.dto;

import java.util.List;

public record MemberAuthorizationResponse(
        boolean member,
        boolean active,
        String roleCode,
        List<String> permissions
) {
}