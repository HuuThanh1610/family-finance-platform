package com.familyfinance.notification.repository;

import com.familyfinance.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    List<NotificationTemplate> findByActiveTrueOrderByTemplateKeyAsc();

    Optional<NotificationTemplate> findFirstByTemplateKeyAndChannelAndLocaleAndActiveTrue(
            String templateKey, String channel, String locale);
}