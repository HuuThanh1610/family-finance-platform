package com.familyfinance.profile.service;

import com.familyfinance.profile.dto.RecordConsentRequest;
import com.familyfinance.profile.dto.UserConsentResponse;
import com.familyfinance.profile.entity.UserConsent;
import com.familyfinance.profile.entity.UserProfile;
import com.familyfinance.profile.repository.UserConsentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserConsentService {

    private final UserConsentRepository repository;

    public UserConsentService(UserConsentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserConsentResponse record(UserProfile profile, RecordConsentRequest request) {
        UserConsent consent = repository
                .findByUserProfileIdAndConsentTypeAndPolicyVersion(
                        profile.getId(), request.consentType(), request.policyVersion())
                .orElseGet(() -> {
                    UserConsent created = new UserConsent();
                    created.setUserProfile(profile);
                    created.setConsentType(request.consentType());
                    created.setPolicyVersion(request.policyVersion());
                    return created;
                });

        boolean granted = Boolean.TRUE.equals(request.granted());
        consent.setGranted(granted);
        Instant now = Instant.now();
        if (granted) {
            consent.setGrantedAt(now);
            consent.setRevokedAt(null);
        } else {
            consent.setGrantedAt(consent.getGrantedAt() != null ? consent.getGrantedAt() : now);
            consent.setRevokedAt(now);
        }
        return toResponse(repository.save(consent));
    }

    private UserConsentResponse toResponse(UserConsent entity) {
        return new UserConsentResponse(
                entity.getId(),
                entity.getConsentType(),
                entity.getPolicyVersion(),
                Boolean.TRUE.equals(entity.getGranted()),
                entity.getGrantedAt(),
                entity.getRevokedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}