package com.familyfinance.profile.service;

import com.familyfinance.profile.dto.PreferenceItemRequest;
import com.familyfinance.profile.dto.UserPreferenceResponse;
import com.familyfinance.profile.entity.UserPreference;
import com.familyfinance.profile.entity.UserProfile;
import com.familyfinance.profile.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserPreferenceService {

    private final UserPreferenceRepository repository;

    public UserPreferenceService(UserPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UserPreferenceResponse> listForProfile(UUID profileId) {
        return repository.findByUserProfileIdOrderByPreferenceKeyAsc(profileId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<UserPreferenceResponse> upsertForProfile(UserProfile profile, List<PreferenceItemRequest> items) {
        return items.stream()
                .map(item -> upsertOne(profile, item))
                .map(this::toResponse)
                .toList();
    }

    private UserPreference upsertOne(UserProfile profile, PreferenceItemRequest item) {
        UserPreference pref = repository
                .findByUserProfileIdAndPreferenceKey(profile.getId(), item.preferenceKey())
                .orElseGet(() -> {
                    UserPreference created = new UserPreference();
                    created.setUserProfile(profile);
                    created.setPreferenceKey(item.preferenceKey());
                    return created;
                });
        pref.setPreferenceValue(item.preferenceValue());
        return repository.save(pref);
    }

    private UserPreferenceResponse toResponse(UserPreference entity) {
        return new UserPreferenceResponse(
                entity.getId(),
                entity.getPreferenceKey(),
                entity.getPreferenceValue(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}