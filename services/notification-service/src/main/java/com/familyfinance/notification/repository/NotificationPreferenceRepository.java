package com.familyfinance.notification.repository;

import com.familyfinance.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByUserProfileId(UUID userProfileId);

    Optional<NotificationPreference> findByUserProfileIdAndChannelAndCategory(
            UUID userProfileId, String channel, String category);
}
