package com.familyfinance.family.service;

import com.familyfinance.family.dto.FamilyMemberResponse;
import com.familyfinance.family.dto.FamilyResponse;
import com.familyfinance.family.dto.InvitationResponse;
import com.familyfinance.family.entity.Family;
import com.familyfinance.family.entity.FamilyInvitation;
import com.familyfinance.family.entity.FamilyMember;
import org.springframework.stereotype.Component;

@Component
public class FamilyMapper {

    public FamilyResponse toFamilyResponse(Family entity) {
        return new FamilyResponse(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public FamilyMemberResponse toMemberResponse(FamilyMember entity) {
        return new FamilyMemberResponse(
                entity.getId(),
                entity.getFamily().getId(),
                entity.getUserProfileId(),
                entity.getRoleCode(),
                entity.getStatus(),
                entity.getNickname(),
                entity.getJoinedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public InvitationResponse toInvitationResponse(FamilyInvitation entity) {
        return new InvitationResponse(
                entity.getId(),
                entity.getFamily().getId(),
                entity.getInviterMemberId(),
                entity.getInviteeEmail(),
                entity.getRoleCode(),
                entity.getInviteToken(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getAcceptedAt(),
                entity.getCreatedAt()
        );
    }
}