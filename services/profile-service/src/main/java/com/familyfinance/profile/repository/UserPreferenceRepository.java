package com.familyfinance.profile.repository;

import com.familyfinance.profile.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    List<UserPreference> findByUserProfileIdOrderByPreferenceKeyAsc(UUID userProfileId);

    Optional<UserPreference> findByUserProfileIdAndPreferenceKey(UUID userProfileId, String preferenceKey);
}