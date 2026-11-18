package com.familyfinance.family.service;

import com.familyfinance.family.dto.CreateInvitationRequest;
import com.familyfinance.family.dto.FamilyMemberResponse;
import com.familyfinance.family.dto.InvitationResponse;
import com.familyfinance.family.entity.Family;
import com.familyfinance.family.entity.FamilyInvitation;
import com.familyfinance.family.entity.FamilyMember;
import com.familyfinance.family.exception.BadRequestException;
import com.familyfinance.family.exception.ConflictException;
import com.familyfinance.family.exception.ResourceNotFoundException;
import com.familyfinance.family.repository.FamilyInvitationRepository;
import com.familyfinance.family.repository.FamilyMemberRepository;
import com.familyfinance.family.repository.FamilyRepository;
import com.familyfinance.family.support.FamilyConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class FamilyInvitationService {

    private static final int INVITE_TTL_DAYS = 7;

    private final FamilyRepository familyRepository;
    private final FamilyInvitationRepository invitationRepository;
    private final FamilyMemberRepository memberRepository;
    private final FamilyAuthorizationService authorizationService;
    private final FamilyAuditService auditService;
    private final FamilyMapper mapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public FamilyInvitationService(
            FamilyRepository familyRepository,
            FamilyInvitationRepository invitationRepository,
            FamilyMemberRepository memberRepository,
            FamilyAuthorizationService authorizationService,
            FamilyAuditService auditService,
            FamilyMapper mapper) {
        this.familyRepository = familyRepository;
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    public InvitationResponse invite(UUID familyId, CreateInvitationRequest request, UUID actorUserId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));

        authorizationService.requireRoleAtLeast(familyId, actorUserId, FamilyConstants.ROLE_PARENT);

        if (FamilyConstants.ROLE_OWNER.equals(request.roleCode())) {
            authorizationService.requireRoleAtLeast(familyId, actorUserId, FamilyConstants.ROLE_OWNER);
        }

        String email = request.inviteeEmail().trim().toLowerCase();
        if (invitationRepository.existsByFamilyIdAndInviteeEmailIgnoreCaseAndStatus(
                familyId, email, FamilyConstants.INVITATION_PENDING)) {
            throw new ConflictException("A pending invitation already exists for this email");
        }

        FamilyMember inviter = authorizationService.findActiveMember(familyId, actorUserId)
                .orElseThrow();

        FamilyInvitation invitation = new FamilyInvitation();
        invitation.setFamily(family);
        invitation.setInviterMemberId(inviter.getId());
        invitation.setInviteeEmail(email);
        invitation.setRoleCode(request.roleCode());
        invitation.setInviteToken(generateToken());
        invitation.setStatus(FamilyConstants.INVITATION_PENDING);
        invitation.setExpiresAt(Instant.now().plusSeconds(INVITE_TTL_DAYS * 24L * 3600L));

        FamilyInvitation saved = invitationRepository.save(invitation);

        auditService.record(
                familyId,
                actorUserId,
                "MEMBER_INVITED",
                "FAMILY_INVITATION",
                saved.getId(),
                Map.of(
                        "inviteeEmail", email,
                        "roleCode", request.roleCode()
                )
        );

        return mapper.toInvitationResponse(saved);
    }

    public FamilyMemberResponse acceptInvitation(String inviteToken, UUID userProfileId) {
        FamilyInvitation invitation = loadActionableInvitation(inviteToken);

        if (memberRepository.existsByFamilyIdAndUserProfileIdAndStatus(
                invitation.getFamily().getId(), userProfileId, FamilyConstants.MEMBER_ACTIVE)) {
            throw new ConflictException("User is already a member of this family");
        }

        FamilyMember member = new FamilyMember();
        member.setFamily(invitation.getFamily());
        member.setUserProfileId(userProfileId);
        member.setRoleCode(invitation.getRoleCode());
        member.setStatus(FamilyConstants.MEMBER_ACTIVE);
        FamilyMember savedMember = memberRepository.save(member);

        invitation.setStatus(FamilyConstants.INVITATION_ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);

        UUID familyId = invitation.getFamily().getId();
        auditService.record(
                familyId,
                userProfileId,
                "INVITATION_ACCEPTED",
                "FAMILY_MEMBER",
                savedMember.getId(),
                Map.of("invitationId", invitation.getId().toString(), "roleCode", invitation.getRoleCode())
        );

        return mapper.toMemberResponse(savedMember);
    }

    public InvitationResponse rejectInvitation(String inviteToken, UUID userProfileId) {
        FamilyInvitation invitation = loadActionableInvitation(inviteToken);

        invitation.setStatus(FamilyConstants.INVITATION_DECLINED);
        FamilyInvitation saved = invitationRepository.save(invitation);

        auditService.record(
                invitation.getFamily().getId(),
                userProfileId,
                "INVITATION_DECLINED",
                "FAMILY_INVITATION",
                saved.getId(),
                Map.of()
        );

        return mapper.toInvitationResponse(saved);
    }

    private FamilyInvitation loadActionableInvitation(String inviteToken) {
        FamilyInvitation invitation = invitationRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found for token"));

        if (!FamilyConstants.INVITATION_PENDING.equals(invitation.getStatus())) {
            throw new BadRequestException("Invitation is no longer pending");
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(FamilyConstants.INVITATION_EXPIRED);
            invitationRepository.save(invitation);
            throw new BadRequestException("Invitation has expired");
        }
        return invitation;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}