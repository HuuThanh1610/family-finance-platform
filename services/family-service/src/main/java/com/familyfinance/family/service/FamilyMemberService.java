package com.familyfinance.family.service;

import com.familyfinance.family.dto.FamilyMemberResponse;
import com.familyfinance.family.dto.InternalFamilyMemberResponse;
import com.familyfinance.family.dto.UpdateMemberRoleRequest;
import com.familyfinance.family.entity.FamilyMember;
import com.familyfinance.family.exception.BadRequestException;
import com.familyfinance.family.exception.ConflictException;
import com.familyfinance.family.exception.ForbiddenException;
import com.familyfinance.family.exception.ResourceNotFoundException;
import com.familyfinance.family.repository.FamilyMemberRepository;
import com.familyfinance.family.support.FamilyConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class FamilyMemberService {

    private final FamilyMemberRepository memberRepository;
    private final FamilyAuthorizationService authorizationService;
    private final FamilyAuditService auditService;
    private final FamilyMapper mapper;

    public FamilyMemberService(
            FamilyMemberRepository memberRepository,
            FamilyAuthorizationService authorizationService,
            FamilyAuditService auditService,
            FamilyMapper mapper) {
        this.memberRepository = memberRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> listMembers(UUID familyId, UUID actorUserId) {
        authorizationService.requireMember(familyId, actorUserId);
        return memberRepository.findByFamilyIdAndStatus(familyId, FamilyConstants.MEMBER_ACTIVE).stream()
                .map(mapper::toMemberResponse)
                .toList();
    }

    /** Internal S2S: active members only (LEFT/REMOVED excluded). */
    @Transactional(readOnly = true)
    public List<InternalFamilyMemberResponse> listActiveMembersInternal(UUID familyId) {
        return memberRepository.findByFamilyIdAndStatus(familyId, FamilyConstants.MEMBER_ACTIVE).stream()
                .map(m -> new InternalFamilyMemberResponse(m.getUserProfileId(), m.getRoleCode(), m.getStatus()))
                .toList();
    }

    public FamilyMemberResponse updateRole(
            UUID familyId, UUID memberId, UpdateMemberRoleRequest request, UUID actorUserId) {
        authorizationService.requireRoleAtLeast(familyId, actorUserId, FamilyConstants.ROLE_PARENT);

        FamilyMember target = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", memberId));
        if (!target.getFamily().getId().equals(familyId)) {
            throw new ResourceNotFoundException("FamilyMember", memberId);
        }
        if (!FamilyConstants.MEMBER_ACTIVE.equals(target.getStatus())) {
            throw new BadRequestException("Member is not active");
        }

        String newRole = request.roleCode();
        String oldRole = target.getRoleCode();

        FamilyMember actor = authorizationService.findActiveMember(familyId, actorUserId)
                .orElseThrow(() -> new ForbiddenException("Not an active member of this family"));

        if (FamilyConstants.ROLE_PARENT.equals(actor.getRoleCode())
                && (FamilyConstants.ROLE_OWNER.equals(newRole) || FamilyConstants.ROLE_OWNER.equals(oldRole))) {
            throw new ForbiddenException("PARENT cannot change OWNER role assignments");
        }

        if (FamilyConstants.ROLE_OWNER.equals(oldRole)
                && !FamilyConstants.ROLE_OWNER.equals(newRole)) {
            long owners = memberRepository.countByFamilyIdAndRoleCodeAndStatus(
                    familyId, FamilyConstants.ROLE_OWNER, FamilyConstants.MEMBER_ACTIVE);
            if (owners <= 1) {
                throw new ConflictException("Cannot change role of the last OWNER");
            }
        }

        if (FamilyConstants.ROLE_OWNER.equals(newRole)
                && !FamilyConstants.ROLE_OWNER.equals(oldRole)) {
            long owners = memberRepository.countByFamilyIdAndRoleCodeAndStatus(
                    familyId, FamilyConstants.ROLE_OWNER, FamilyConstants.MEMBER_ACTIVE);
            if (owners >= 1 && !FamilyConstants.ROLE_OWNER.equals(oldRole)) {
                // allow multiple owners if business permits — single-owner default: only transfer via explicit flow
                // For now allow promoting to OWNER when actor is OWNER
                if (!FamilyConstants.ROLE_OWNER.equals(actor.getRoleCode())) {
                    throw new ForbiddenException("Only OWNER can promote another member to OWNER");
                }
            }
        }

        target.setRoleCode(newRole);
        FamilyMember saved = memberRepository.save(target);

        auditService.record(
                familyId,
                actorUserId,
                "MEMBER_ROLE_UPDATED",
                "FAMILY_MEMBER",
                saved.getId(),
                Map.of(
                        "userProfileId", saved.getUserProfileId().toString(),
                        "fromRole", oldRole,
                        "toRole", newRole
                )
        );

        return mapper.toMemberResponse(saved);
    }

    public void removeMember(UUID familyId, UUID memberId, UUID actorUserId) {
        authorizationService.requireRoleAtLeast(familyId, actorUserId, FamilyConstants.ROLE_PARENT);

        FamilyMember target = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", memberId));
        if (!target.getFamily().getId().equals(familyId)) {
            throw new ResourceNotFoundException("FamilyMember", memberId);
        }
        if (!FamilyConstants.MEMBER_ACTIVE.equals(target.getStatus())) {
            throw new BadRequestException("Member is not active");
        }

        if (target.getUserProfileId().equals(actorUserId)) {
            throw new BadRequestException("Use leave flow instead of removing yourself");
        }

        if (FamilyConstants.ROLE_OWNER.equals(target.getRoleCode())) {
            long owners = memberRepository.countByFamilyIdAndRoleCodeAndStatus(
                    familyId, FamilyConstants.ROLE_OWNER, FamilyConstants.MEMBER_ACTIVE);
            if (owners <= 1) {
                throw new ConflictException("Cannot remove the last OWNER");
            }
        }

        target.setStatus(FamilyConstants.MEMBER_REMOVED);
        memberRepository.save(target);

        auditService.record(
                familyId,
                actorUserId,
                "MEMBER_REMOVED",
                "FAMILY_MEMBER",
                target.getId(),
                Map.of("userProfileId", target.getUserProfileId().toString(), "roleCode", target.getRoleCode())
        );
    }
}