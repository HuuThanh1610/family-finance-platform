package com.familyfinance.profile.service;

import com.familyfinance.profile.dto.UserProfileResponse;
import com.familyfinance.profile.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfileResponse toResponse(UserProfile entity) {
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