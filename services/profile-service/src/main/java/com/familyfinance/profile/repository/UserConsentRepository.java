package com.familyfinance.profile.repository;

import com.familyfinance.profile.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {

    Optional<UserConsent> findByUserProfileIdAndConsentTypeAndPolicyVersion(
            UUID userProfileId, String consentType, String policyVersion);
}