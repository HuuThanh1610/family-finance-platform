package com.familyfinance.profile.service;

import com.familyfinance.profile.dto.ProvisionProfileRequest;
import com.familyfinance.profile.dto.UserProfileResponse;
import com.familyfinance.profile.entity.UserProfile;
import com.familyfinance.profile.exception.ConflictException;
import com.familyfinance.profile.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProfileProvisioningService {

    private final UserProfileRepository repository;
    private final UserProfileMapper mapper;

    public ProfileProvisioningService(UserProfileRepository repository, UserProfileMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public UserProfileResponse provision(ProvisionProfileRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        return repository.findByKeycloakSub(request.keycloakSub())
                .map(existing -> updateExisting(existing, normalizedEmail, request.displayName().trim()))
                .orElseGet(() -> createNew(request.keycloakSub().trim(), normalizedEmail, request.displayName().trim()));
    }

    private UserProfileResponse createNew(String keycloakSub, String email, String displayName) {
        repository.findByEmail(email).ifPresent(other -> {
            throw new ConflictException("Email already registered: " + email);
        });
        UserProfile profile = new UserProfile();
        // Align profile.id with Keycloak sub when sub is a UUID so family/wallet
        // CurrentUserResolver (JWT sub → UUID) matches the provisioned profile.
        try {
            profile.setId(UUID.fromString(keycloakSub));
        } catch (IllegalArgumentException ignored) {
            // non-UUID subs keep GenerationType.UUID
        }
        profile.setKeycloakSub(keycloakSub);
        profile.setEmail(email);
        profile.setDisplayName(displayName);
        return mapper.toResponse(repository.save(profile));
    }

    private UserProfileResponse updateExisting(UserProfile existing, String email, String displayName) {
        if (!existing.getEmail().equalsIgnoreCase(email)) {
            repository.findByEmail(email)
                    .filter(other -> !other.getId().equals(existing.getId()))
                    .ifPresent(other -> {
                        throw new ConflictException("Email already registered: " + email);
                    });
            existing.setEmail(email);
        }
        existing.setDisplayName(displayName);
        return mapper.toResponse(repository.save(existing));
    }
}