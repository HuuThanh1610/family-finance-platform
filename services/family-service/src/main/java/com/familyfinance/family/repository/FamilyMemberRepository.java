package com.familyfinance.family.repository;

import com.familyfinance.family.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {

    Optional<FamilyMember> findByFamilyIdAndUserProfileIdAndStatus(
            UUID familyId, UUID userProfileId, String status);

    List<FamilyMember> findByFamilyIdAndStatus(UUID familyId, String status);

    List<FamilyMember> findByUserProfileIdAndStatus(UUID userProfileId, String status);

    long countByFamilyIdAndRoleCodeAndStatus(UUID familyId, String roleCode, String status);

    boolean existsByFamilyIdAndUserProfileIdAndStatus(UUID familyId, UUID userProfileId, String status);
}