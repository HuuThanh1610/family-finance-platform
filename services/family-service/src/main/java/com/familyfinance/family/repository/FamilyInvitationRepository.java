package com.familyfinance.family.repository;

import com.familyfinance.family.entity.FamilyInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyInvitationRepository extends JpaRepository<FamilyInvitation, UUID> {

    Optional<FamilyInvitation> findByInviteToken(String inviteToken);

    boolean existsByFamilyIdAndInviteeEmailIgnoreCaseAndStatus(
            UUID familyId, String inviteeEmail, String status);

    List<FamilyInvitation> findByFamilyIdAndStatus(UUID familyId, String status);
}