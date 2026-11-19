package com.familyfinance.family.service;

import com.familyfinance.family.dto.InternalFamilyMemberResponse;
import com.familyfinance.family.entity.FamilyMember;
import com.familyfinance.family.repository.FamilyMemberRepository;
import com.familyfinance.family.support.FamilyConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyMemberServiceInternalTest {

    @Mock FamilyMemberRepository memberRepository;
    @Mock FamilyAuthorizationService authorizationService;
    @Mock FamilyAuditService auditService;
    @Mock FamilyMapper mapper;

    FamilyMemberService service;

    @BeforeEach
    void setUp() {
        service = new FamilyMemberService(memberRepository, authorizationService, auditService, mapper);
    }

    @Test
    void listActiveMembersInternalReturnsOnlyActiveRows() {
        UUID familyId = UUID.randomUUID();
        UUID ownerProfile = UUID.randomUUID();
        FamilyMember owner = new FamilyMember();
        owner.setUserProfileId(ownerProfile);
        owner.setRoleCode(FamilyConstants.ROLE_OWNER);
        owner.setStatus(FamilyConstants.MEMBER_ACTIVE);

        when(memberRepository.findByFamilyIdAndStatus(familyId, FamilyConstants.MEMBER_ACTIVE))
                .thenReturn(List.of(owner));

        List<InternalFamilyMemberResponse> result = service.listActiveMembersInternal(familyId);

        assertThat(result).containsExactly(
                new InternalFamilyMemberResponse(ownerProfile, FamilyConstants.ROLE_OWNER, FamilyConstants.MEMBER_ACTIVE));
        verify(memberRepository).findByFamilyIdAndStatus(familyId, FamilyConstants.MEMBER_ACTIVE);
    }
}
