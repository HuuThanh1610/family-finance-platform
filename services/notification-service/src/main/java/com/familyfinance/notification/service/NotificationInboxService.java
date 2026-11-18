package com.familyfinance.notification.service;

import com.familyfinance.notification.entity.InboxEvent;
import com.familyfinance.notification.entity.Notification;
import com.familyfinance.notification.messaging.DomainEventMessage;
import com.familyfinance.notification.repository.InboxEventRepository;
import com.familyfinance.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.familyfinance.notification.service.NotificationEventMapper.NotificationDraft;

@Service
public class NotificationInboxService {

    private static final Logger log = LoggerFactory.getLogger(NotificationInboxService.class);

    private final InboxEventRepository inboxEventRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService preferenceService;

    public NotificationInboxService(
            InboxEventRepository inboxEventRepository,
            NotificationRepository notificationRepository,
            NotificationPreferenceService preferenceService) {
        this.inboxEventRepository = inboxEventRepository;
        this.notificationRepository = notificationRepository;
        this.preferenceService = preferenceService;
    }

    @Transactional
    public void process(DomainEventMessage event) {
        if (inboxEventRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate notification event skipped eventId={}", event.eventId());
            return;
        }

        Instant now = Instant.now();
        InboxEvent inbox = new InboxEvent();
        inbox.setEventId(event.eventId());
        inbox.setEventType(event.eventType());
        inbox.setAggregateType(event.aggregateType());
        inbox.setAggregateId(event.aggregateId());
        inbox.setPayload(event.payload() != null ? event.payload() : Map.of());
        inbox.setReceivedAt(now);
        inbox.setStatus("PENDING");
        inboxEventRepository.save(inbox);

        try {
            var draft = NotificationEventMapper.toDraft(event);
            if (draft.isPresent()) {
                NotificationDraft d = draft.get();
                if (preferenceService.isChannelEnabled(d.recipientUserId(), d.channel(), categoryFromTemplate(d.templateKey()))) {
                    Notification n = new Notification();
                    n.setRecipientUserId(d.recipientUserId());
                    n.setFamilyId(d.familyId());
                    n.setTemplateKey(d.templateKey());
                    n.setChannel(d.channel());
                    n.setTitle(d.title());
                    n.setBody(d.body());
                    n.setPayload(new HashMap<>(d.payload()));
                    n.setStatus("DELIVERED");
                    n.setCreatedAt(now);
                    n.setUpdatedAt(now);
                    notificationRepository.save(n);
                }
            }
            inbox.setStatus("PROCESSED");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
        } catch (RuntimeException ex) {
            inbox.setStatus("FAILED");
            inboxEventRepository.save(inbox);
            throw ex;
        }
    }

    private static String categoryFromTemplate(String templateKey) {
        return switch (templateKey) {
            case "ALLOWANCE" -> "ALLOWANCE";
            case "EXPENSE", "PAYMENT" -> "BUDGET";
            case "INVENTORY_LOW" -> "HOUSEHOLD";
            default -> "SYSTEM";
        };
    }
}