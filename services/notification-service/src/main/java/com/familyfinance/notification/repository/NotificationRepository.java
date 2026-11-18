package com.familyfinance.notification.repository;

import com.familyfinance.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId);

    List<Notification> findByRecipientUserIdAndFamilyIdOrderByCreatedAtDesc(UUID recipientUserId, UUID familyId);
}
