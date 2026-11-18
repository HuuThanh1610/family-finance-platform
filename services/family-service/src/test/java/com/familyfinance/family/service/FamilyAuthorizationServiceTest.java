package com.familyfinance.family.service;

import com.familyfinance.family.dto.MemberAuthorizationResponse;
import com.familyfinance.family.entity.Family;
import com.familyfinance.family.entity.FamilyMember;
import com.familyfinance.family.entity.FamilyPermission;
import com.familyfinance.family.entity.FamilyRole;
import com.familyfinance.family.exception.ForbiddenException;
import com.familyfinance.family.repository.FamilyMemberRepository;
import com.familyfinance.family.repository.FamilyPermissionRepository;
import com.familyfinance.family.repository.FamilyRoleRepository;
import com.familyfinance.family.support.FamilyConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyAuthorizationServiceTest {

    @Mock
    private FamilyMemberRepository memberRepository;

    @Mock
    private FamilyPermissionRepository permissionRepository;

    @Mock
    private FamilyRoleRepository roleRepository;

    private FamilyAuthorizationService service;

    private final UUID familyId = UUID.fromString("a1000000-0000-4000-8000-000000000001");
    private final UUID userId = UUID.fromString("b1000000-0000-4000-8000-000000000001");

    @BeforeEach
    void setUp() {
        service = new FamilyAuthorizationService(memberRepository, permissionRepository, roleRepository);
    }

    @Test
    void isMember_returnsTrueForActiveMembership() {
        when(memberRepository.existsByFamilyIdAndUserProfileIdAndStatus(
                familyId, userId, FamilyConstants.MEMBER_ACTIVE))
                .thenReturn(true);

        assertTrue(service.isMember(familyId, userId));
    }

    @Test
    void isMember_returnsFalseWhenNotMember() {
        when(memberRepository.existsByFamilyIdAndUserProfileIdAndStatus(
                familyId, userId, FamilyConstants.MEMBER_ACTIVE))
                .thenReturn(false);

        assertFalse(service.isMember(familyId, userId));
    }

    @Test
    void hasRoleAtLeast_ownerMeetsParentThreshold() {
        stubActiveMember(FamilyConstants.ROLE_OWNER);
        when(roleRepository.findById(FamilyConstants.ROLE_OWNER))
                .thenReturn(Optional.of(role(FamilyConstants.ROLE_OWNER, 10)));
        when(roleRepository.findById(FamilyConstants.ROLE_PARENT))
                .thenReturn(Optional.of(role(FamilyConstants.ROLE_PARENT, 20)));

        assertTrue(service.hasRoleAtLeast(familyId, userId, FamilyConstants.ROLE_PARENT));
    }

    @Test
    void hasRoleAtLeast_childDoesNotMeetParentThreshold() {
        stubActiveMember(FamilyConstants.ROLE_CHILD);
        when(roleRepository.findById(FamilyConstants.ROLE_CHILD))
                .thenReturn(Optional.of(role(FamilyConstants.ROLE_CHILD, 40)));
        when(roleRepository.findById(FamilyConstants.ROLE_PARENT))
                .thenReturn(Optional.of(role(FamilyConstants.ROLE_PARENT, 20)));

        assertFalse(service.hasRoleAtLeast(familyId, userId, FamilyConstants.ROLE_PARENT));
    }

    @Test
    void hasRoleAtLeast_failClosedWhenRoleUnknown() {
        stubActiveMember("UNKNOWN_ROLE");
        when(roleRepository.findById("UNKNOWN_ROLE")).thenReturn(Optional.empty());
        when(roleRepository.findById(FamilyConstants.ROLE_PARENT))
                .thenReturn(Optional.of(role(FamilyConstants.ROLE_PARENT, 20)));

        assertFalse(service.hasRoleAtLeast(familyId, userId, FamilyConstants.ROLE_PARENT));
    }

    @Test
    void requirePermission_grantsWhenPermissionExists() {
        stubActiveMember(FamilyConstants.ROLE_OWNER);
        when(permissionRepository.existsByRoleCodeAndPermissionKey(
                FamilyConstants.ROLE_OWNER, FamilyConstants.PERMISSION_FAMILY_MANAGE))
                .thenReturn(true);

        service.requirePermission(familyId, userId, FamilyConstants.PERMISSION_FAMILY_MANAGE);

        verify(permissionRepository)
                .existsByRoleCodeAndPermissionKey(
                        FamilyConstants.ROLE_OWNER, FamilyConstants.PERMISSION_FAMILY_MANAGE);
    }

    @Test
    void requirePermission_throwsWhenNotMember() {
        when(memberRepository.findByFamilyIdAndUserProfileIdAndStatus(
                familyId, userId, FamilyConstants.MEMBER_ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () ->
                service.requirePermission(familyId, userId, FamilyConstants.PERMISSION_FAMILY_MANAGE));

        verify(permissionRepository, never()).existsByRoleCodeAndPermissionKey(any(), any());
    }

    @Test
    void requirePermission_throwsWhenPermissionMissing() {
        stubActiveMember(FamilyConstants.ROLE_VIEWER);
        when(permissionRepository.existsByRoleCodeAndPermissionKey(
                FamilyConstants.ROLE_VIEWER, FamilyConstants.PERMISSION_FAMILY_MANAGE))
                .thenReturn(false);

        assertThrows(ForbiddenException.class, () ->
                service.requirePermission(familyId, userId, FamilyConstants.PERMISSION_FAMILY_MANAGE));
    }

    @Test
    void resolveAuthorization_failClosedForNonMember() {
        when(memberRepository.findByFamilyIdAndUserProfileIdAndStatus(
                familyId, userId, FamilyConstants.MEMBER_ACTIVE))
                .thenReturn(Optional.empty());

        MemberAuthorizationResponse response = service.resolveAuthorization(familyId, userId);

        assertFalse(response.member());
        assertFalse(response.active());
        assertEquals(List.of(), response.permissions());
    }

    @Test
    void resolveAuthorization_returnsRoleAndPermissions() {
        stubActiveMember(FamilyConstants.ROLE_PARENT);
        FamilyPermission p1 = new FamilyPermission();
        p1.setPermissionKey("budget.approve");
        FamilyPermission p2 = new FamilyPermission();
        p2.setPermissionKey("expense.create");
        when(permissionRepository.findByRoleCode(FamilyConstants.ROLE_PARENT))
                .thenReturn(List.of(p1, p2));

        MemberAuthorizationResponse response = service.resolveAuthorization(familyId, userId);

        assertTrue(response.member());
        assertEquals(FamilyConstants.ROLE_PARENT, response.roleCode());
        assertEquals(List.of("budget.approve", "expense.create"), response.permissions());
    }

    @Test
    void requireMember_blocksCrossFamilyUser() {
        UUID otherFamily = UUID.fromString("a1000000-0000-4000-8000-000000000099");
        when(memberRepository.existsByFamilyIdAndUserProfileIdAndStatus(
                otherFamily, userId, FamilyConstants.MEMBER_ACTIVE))
                .thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.requireMember(otherFamily, userId));
    }

    @Test
    void viewerCannotManageFamily() {
        stubActiveMember(FamilyConstants.ROLE_VIEWER);
        when(permissionRepository.existsByRoleCodeAndPermissionKey(
                FamilyConstants.ROLE_VIEWER, FamilyConstants.PERMISSION_FAMILY_MANAGE))
                .thenReturn(false);

        assertThrows(ForbiddenException.class, () ->
                service.requirePermission(familyId, userId, FamilyConstants.PERMISSION_FAMILY_MANAGE));
    }

    @Test
    void childCannotReachParentRole() {
        stubActiveMember(FamilyConstants.ROLE_CHILD);
        when(roleRepository.findById(FamilyConstants.ROLE_CHILD))
                .thenReturn(Optional.of(role(FamilyConstants.ROLE_CHILD, 40)));
        when(roleRepository.findById(FamilyConstants.ROLE_PARENT))
                .thenReturn(Optional.of(role(FamilyConstants.ROLE_PARENT, 20)));

        assertThrows(ForbiddenException.class, () ->
                service.requireRoleAtLeast(familyId, userId, FamilyConstants.ROLE_PARENT));
    }

    private void stubActiveMember(String roleCode) {
        Family family = new Family();
        family.setId(familyId);
        FamilyMember member = new FamilyMember();
        member.setFamily(family);
        member.setUserProfileId(userId);
        member.setRoleCode(roleCode);
        member.setStatus(FamilyConstants.MEMBER_ACTIVE);
        when(memberRepository.findByFamilyIdAndUserProfileIdAndStatus(
                eq(familyId), eq(userId), eq(FamilyConstants.MEMBER_ACTIVE)))
                .thenReturn(Optional.of(member));
    }

    private static FamilyRole role(String code, int sortOrder) {
        FamilyRole role = new FamilyRole();
        role.setCode(code);
        role.setSortOrder(sortOrder);
        return role;
    }
}