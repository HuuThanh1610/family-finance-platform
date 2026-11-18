package com.familyfinance.notification.service;

import com.familyfinance.notification.dto.NotificationPreferenceResponse;
import com.familyfinance.notification.dto.NotificationResponse;
import com.familyfinance.notification.dto.UpsertPreferenceRequest;
import com.familyfinance.notification.entity.InboxEvent;
import com.familyfinance.notification.entity.Notification;
import com.familyfinance.notification.entity.NotificationPreference;
import com.familyfinance.notification.exception.ResourceNotFoundException;
import com.familyfinance.notification.repository.InboxEventRepository;
import com.familyfinance.notification.repository.NotificationPreferenceRepository;
import com.familyfinance.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationApplicationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final InboxEventRepository inboxEventRepository;

    public NotificationApplicationService(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            InboxEventRepository inboxEventRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.inboxEventRepository = inboxEventRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(UUID recipientUserId, UUID familyId) {
        var rows = familyId == null
                ? notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId)
                : notificationRepository.findByRecipientUserIdAndFamilyIdOrderByCreatedAtDesc(recipientUserId, familyId);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional
    public int markAllRead(UUID recipientUserId) {
        int count = 0;
        Instant now = Instant.now();
        for (Notification n : notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId)) {
            if (!"READ".equals(n.getStatus())) {
                n.setStatus("READ");
                n.setUpdatedAt(now);
                notificationRepository.save(n);
                count++;
            }
        }
        return count;
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        n.setStatus("READ");
        n.setUpdatedAt(Instant.now());
        return toResponse(notificationRepository.save(n));
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> preferences(UUID userProfileId) {
        return preferenceRepository.findByUserProfileId(userProfileId).stream()
                .map(p -> new NotificationPreferenceResponse(
                        p.getId(), p.getUserProfileId(), p.getChannel(), p.getCategory(), p.isEnabled()))
                .toList();
    }

    @Transactional
    public NotificationPreferenceResponse upsertPreference(UpsertPreferenceRequest request) {
        Instant now = Instant.now();
        NotificationPreference pref = preferenceRepository
                .findByUserProfileIdAndChannelAndCategory(
                        request.userProfileId(), request.channel(), request.category())
                .orElseGet(NotificationPreference::new);
        if (pref.getId() == null) {
            pref.setUserProfileId(request.userProfileId());
            pref.setChannel(request.channel());
            pref.setCategory(request.category());
            pref.setCreatedAt(now);
        }
        pref.setEnabled(Boolean.TRUE.equals(request.enabled()));
        pref.setUpdatedAt(now);
        pref = preferenceRepository.save(pref);
        return new NotificationPreferenceResponse(
                pref.getId(), pref.getUserProfileId(), pref.getChannel(), pref.getCategory(), pref.isEnabled());
    }

    /**
     * Idempotent domain-event → in-app notification. Kafka wiring can call this.
     */
    @Transactional
    public void ingestEvent(UUID eventId, String eventType, String aggregateType, UUID aggregateId,
                            Map<String, Object> payload, UUID recipientUserId, String title, String body) {
        if (inboxEventRepository.existsByEventId(eventId)) {
            return;
        }
        Instant now = Instant.now();
        InboxEvent inbox = new InboxEvent();
        inbox.setEventId(eventId);
        inbox.setEventType(eventType);
        inbox.setAggregateType(aggregateType);
        inbox.setAggregateId(aggregateId);
        inbox.setPayload(payload != null ? payload : Map.of());
        inbox.setReceivedAt(now);
        inbox.setStatus("PENDING");
        inboxEventRepository.save(inbox);

        Notification n = new Notification();
        n.setRecipientUserId(recipientUserId);
        Object family = payload != null ? payload.get("familyId") : null;
        if (family != null) {
            n.setFamilyId(UUID.fromString(family.toString()));
        }
        n.setTemplateKey(eventType);
        n.setChannel("IN_APP");
        n.setTitle(title);
        n.setBody(body);
        n.setPayload(payload != null ? payload : Map.of());
        n.setStatus("DELIVERED");
        n.setCreatedAt(now);
        n.setUpdatedAt(now);
        notificationRepository.save(n);

        inbox.setStatus("PROCESSED");
        inbox.setProcessedAt(Instant.now());
        inboxEventRepository.save(inbox);
    }

    private NotificationResponse toResponse(Notification entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getRecipientUserId(),
                entity.getFamilyId(),
                entity.getTemplateKey(),
                entity.getChannel(),
                entity.getTitle(),
                entity.getBody(),
                entity.getStatus(),
                "READ".equals(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
