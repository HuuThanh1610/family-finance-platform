package com.familyfinance.profile.service;

import com.familyfinance.profile.dto.UserProfileResponse;
import com.familyfinance.profile.entity.UserProfile;
import com.familyfinance.profile.exception.ResourceNotFoundException;
import com.familyfinance.profile.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    public List<UserProfileResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public UserProfileResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", id));
    }

        private UserProfileResponse toResponse(UserProfile entity) {
        return new UserProfileResponse(
                entity.getId(),
                entity.getKeycloakSub(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getPhone(),
                entity.getAvatarUrl(),
                entity.getLocale(),
                entity.getTimezone(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
