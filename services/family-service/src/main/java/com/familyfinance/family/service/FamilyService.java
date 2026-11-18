package com.familyfinance.family.service;

import com.familyfinance.family.dto.CreateFamilyRequest;
import com.familyfinance.family.dto.FamilyResponse;
import com.familyfinance.family.entity.Family;
import com.familyfinance.family.entity.FamilyMember;
import com.familyfinance.family.exception.ForbiddenException;
import com.familyfinance.family.exception.ResourceNotFoundException;
import com.familyfinance.family.repository.FamilyMemberRepository;
import com.familyfinance.family.repository.FamilyRepository;
import com.familyfinance.family.support.FamilyConstants;
import com.familyfinance.family.support.SlugUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository memberRepository;
    private final FamilyAuthorizationService authorizationService;
    private final FamilyAuditService auditService;
    private final FamilyMapper mapper;

    public FamilyService(
            FamilyRepository familyRepository,
            FamilyMemberRepository memberRepository,
            FamilyAuthorizationService authorizationService,
            FamilyAuditService auditService,
            FamilyMapper mapper) {
        this.familyRepository = familyRepository;
        this.memberRepository = memberRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.mapper = mapper;
    }

    @Transactional
    public FamilyResponse create(CreateFamilyRequest request, UUID creatorUserId) {
        String slug = resolveUniqueSlug(request.slug(), request.name());

        Family family = new Family();
        family.setName(request.name().trim());
        family.setSlug(slug);
        family.setDescription(request.description());
        family.setStatus(FamilyConstants.STATUS_ACTIVE);
        Family savedFamily = familyRepository.save(family);

        FamilyMember owner = new FamilyMember();
        owner.setFamily(savedFamily);
        owner.setUserProfileId(creatorUserId);
        owner.setRoleCode(FamilyConstants.ROLE_OWNER);
        owner.setStatus(FamilyConstants.MEMBER_ACTIVE);
        memberRepository.save(owner);

        auditService.record(
                savedFamily.getId(),
                creatorUserId,
                "FAMILY_CREATED",
                "FAMILY",
                savedFamily.getId(),
                Map.of("name", savedFamily.getName(), "slug", savedFamily.getSlug())
        );

        return mapper.toFamilyResponse(savedFamily);
    }

    @Transactional(readOnly = true)
    public List<FamilyResponse> listForUser(UUID userProfileId) {
        List<UUID> familyIds = memberRepository
                .findByUserProfileIdAndStatus(userProfileId, FamilyConstants.MEMBER_ACTIVE).stream()
                .map(m -> m.getFamily().getId())
                .distinct()
                .toList();
        if (familyIds.isEmpty()) {
            return List.of();
        }
        return familyRepository.findByIdInAndStatus(familyIds, FamilyConstants.STATUS_ACTIVE).stream()
                .map(mapper::toFamilyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FamilyResponse findById(UUID id, UUID userProfileId) {
        if (!authorizationService.isMember(id, userProfileId)) {
            throw new ForbiddenException("Not an active member of this family");
        }
        Family family = familyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Family", id));
        return mapper.toFamilyResponse(family);
    }

    private String resolveUniqueSlug(String requestedSlug, String name) {
        String base = requestedSlug != null && !requestedSlug.isBlank()
                ? SlugUtils.slugify(requestedSlug)
                : SlugUtils.slugify(name);
        String candidate = base;
        int suffix = 2;
        while (familyRepository.existsBySlug(candidate)) {
            String suffixPart = "-" + suffix++;
            int maxBaseLen = Math.max(1, 120 - suffixPart.length());
            String truncated = base.length() > maxBaseLen ? base.substring(0, maxBaseLen) : base;
            candidate = truncated + suffixPart;
        }
        return candidate;
    }
}