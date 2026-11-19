package com.familyfinance.notification.service;

import com.familyfinance.notification.client.FamilyMemberClient;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.familyfinance.notification.service.NotificationEventMapper.NotificationDraft;

@Service
public class NotificationInboxService {

    private static final Logger log = LoggerFactory.getLogger(NotificationInboxService.class);

    private final InboxEventRepository inboxEventRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService preferenceService;
    private final FamilyMemberClient familyMemberClient;

    public NotificationInboxService(
            InboxEventRepository inboxEventRepository,
            NotificationRepository notificationRepository,
            NotificationPreferenceService preferenceService,
            FamilyMemberClient familyMemberClient) {
        this.inboxEventRepository = inboxEventRepository;
        this.notificationRepository = notificationRepository;
        this.preferenceService = preferenceService;
        this.familyMemberClient = familyMemberClient;
    }

    @Transactional
    public void process(DomainEventMessage event) {
        if (inboxEventRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate notification event skipped eventId={}", event.eventId());
            return;
        }

        if ("InventoryLow".equals(event.eventType())) {
            processInventoryLow(event);
            return;
        }

        Instant now = Instant.now();
        InboxEvent inbox = newInbox(event, now);
        inboxEventRepository.save(inbox);

        try {
            var draft = NotificationEventMapper.toDraft(event);
            if (draft.isPresent()) {
                saveIfEnabled(draft.get(), now);
            }
            markProcessed(inbox);
        } catch (RuntimeException ex) {
            inbox.setStatus("FAILED");
            inboxEventRepository.save(inbox);
            throw ex;
        }
    }

    /**
     * InventoryLow has no recipient in payload. Resolve OWNER/PARENT via Family internal API
     * before claiming the inbox row so temporary lookup failures remain Kafka-retryable.
     */
    private void processInventoryLow(DomainEventMessage event) {
        Map<String, Object> payload = event.payload() != null ? event.payload() : Map.of();
        UUID familyId = NotificationEventMapper.uuidFromPayload(payload, "familyId");
        Instant now = Instant.now();

        if (familyId == null) {
            log.warn("InventoryLow missing familyId eventId={}", event.eventId());
            InboxEvent inbox = newInbox(event, now);
            inbox.setStatus("SKIPPED");
            inbox.setProcessedAt(now);
            inboxEventRepository.save(inbox);
            return;
        }

        List<UUID> recipients = InventoryLowRecipientPolicy.resolveRecipients(
                familyMemberClient.listActiveMembers(familyId));

        InboxEvent inbox = newInbox(event, now);
        inboxEventRepository.save(inbox);

        try {
            for (UUID recipient : recipients) {
                NotificationDraft draft = NotificationEventMapper.draftForRecipient(event, recipient, familyId);
                saveIfEnabled(draft, now);
            }
            markProcessed(inbox);
        } catch (RuntimeException ex) {
            inbox.setStatus("FAILED");
            inboxEventRepository.save(inbox);
            throw ex;
        }
    }

    private void saveIfEnabled(NotificationDraft d, Instant now) {
        if (!preferenceService.isChannelEnabled(d.recipientUserId(), d.channel(), categoryFromTemplate(d.templateKey()))) {
            return;
        }
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

    private InboxEvent newInbox(DomainEventMessage event, Instant now) {
        InboxEvent inbox = new InboxEvent();
        inbox.setEventId(event.eventId());
        inbox.setEventType(event.eventType());
        inbox.setAggregateType(event.aggregateType());
        inbox.setAggregateId(event.aggregateId());
        inbox.setPayload(event.payload() != null ? event.payload() : Map.of());
        inbox.setReceivedAt(now);
        inbox.setStatus("PENDING");
        return inbox;
    }

    private void markProcessed(InboxEvent inbox) {
        inbox.setStatus("PROCESSED");
        inbox.setProcessedAt(Instant.now());
        inboxEventRepository.save(inbox);
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
