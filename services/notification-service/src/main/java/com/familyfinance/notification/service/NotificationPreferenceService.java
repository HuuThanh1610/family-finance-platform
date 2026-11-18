package com.familyfinance.notification.service;

import com.familyfinance.notification.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceService(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    public boolean isChannelEnabled(UUID userProfileId, String channel, String category) {
        return repository.findByUserProfileIdAndChannelAndCategory(userProfileId, channel, category)
                .map(p -> p.isEnabled())
                .orElse(true);
    }
}