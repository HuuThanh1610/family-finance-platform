package com.familyfinance.family.service;

import com.familyfinance.family.dto.MemberAuthorizationResponse;
import com.familyfinance.family.entity.FamilyMember;
import com.familyfinance.family.entity.FamilyPermission;
import com.familyfinance.family.entity.FamilyRole;
import com.familyfinance.family.exception.ForbiddenException;
import com.familyfinance.family.repository.FamilyMemberRepository;
import com.familyfinance.family.repository.FamilyPermissionRepository;
import com.familyfinance.family.repository.FamilyRoleRepository;
import com.familyfinance.family.support.FamilyConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FamilyAuthorizationService {

    private final FamilyMemberRepository memberRepository;
    private final FamilyPermissionRepository permissionRepository;
    private final FamilyRoleRepository roleRepository;

    public FamilyAuthorizationService(
            FamilyMemberRepository memberRepository,
            FamilyPermissionRepository permissionRepository,
            FamilyRoleRepository roleRepository) {
        this.memberRepository = memberRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    public boolean isMember(UUID familyId, UUID userProfileId) {
        return memberRepository.existsByFamilyIdAndUserProfileIdAndStatus(
                familyId, userProfileId, FamilyConstants.MEMBER_ACTIVE);
    }

    public boolean hasRoleAtLeast(UUID familyId, UUID userProfileId, String minimumRoleCode) {
        return findActiveMember(familyId, userProfileId)
                .map(member -> compareRoleAtLeast(member.getRoleCode(), minimumRoleCode))
                .orElse(false);
    }

    public void requireMember(UUID familyId, UUID userProfileId) {
        if (!isMember(familyId, userProfileId)) {
            throw new ForbiddenException("Not an active member of this family");
        }
    }

    public void requireRoleAtLeast(UUID familyId, UUID userProfileId, String minimumRoleCode) {
        if (!hasRoleAtLeast(familyId, userProfileId, minimumRoleCode)) {
            throw new ForbiddenException("Insufficient role for this action");
        }
    }

    public void requirePermission(UUID familyId, UUID userProfileId, String permissionKey) {
        FamilyMember member = findActiveMember(familyId, userProfileId)
                .orElseThrow(() -> new ForbiddenException("Not an active member of this family"));
        boolean allowed = permissionRepository.existsByRoleCodeAndPermissionKey(member.getRoleCode(), permissionKey);
        if (!allowed) {
            throw new ForbiddenException("Missing permission: " + permissionKey);
        }
    }

    public MemberAuthorizationResponse resolveAuthorization(UUID familyId, UUID userProfileId) {
        Optional<FamilyMember> memberOpt = findActiveMember(familyId, userProfileId);
        if (memberOpt.isEmpty()) {
            return new MemberAuthorizationResponse(false, false, null, List.of());
        }
        FamilyMember member = memberOpt.get();
        List<String> permissions = permissionRepository.findByRoleCode(member.getRoleCode()).stream()
                .map(FamilyPermission::getPermissionKey)
                .sorted()
                .toList();
        return new MemberAuthorizationResponse(true, true, member.getRoleCode(), permissions);
    }

    public Optional<FamilyMember> findActiveMember(UUID familyId, UUID userProfileId) {
        return memberRepository.findByFamilyIdAndUserProfileIdAndStatus(
                familyId, userProfileId, FamilyConstants.MEMBER_ACTIVE);
    }

    private boolean compareRoleAtLeast(String actualRoleCode, String minimumRoleCode) {
        int actualOrder = roleSortOrder(actualRoleCode);
        int minimumOrder = roleSortOrder(minimumRoleCode);
        if (actualOrder == Integer.MAX_VALUE || minimumOrder == Integer.MAX_VALUE) {
            return false;
        }
        return actualOrder <= minimumOrder;
    }

    private int roleSortOrder(String roleCode) {
        return roleRepository.findById(roleCode)
                .map(FamilyRole::getSortOrder)
                .orElse(Integer.MAX_VALUE);
    }
}