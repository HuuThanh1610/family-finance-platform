package com.familyfinance.analytics.service;

import com.familyfinance.analytics.entity.InboxEvent;
import com.familyfinance.analytics.messaging.DomainEventMessage;
import com.familyfinance.analytics.projection.AnalyticsProjectionService;
import com.familyfinance.analytics.repository.InboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Inbox + projection in one transaction: at-least-once Kafka with effectively-once effects
 * via {@code inbox_events.event_id} and {@code realized_financial_postings.ledger_transaction_id}.
 * <p>
 * {@code FAILED} rows are eligible for retry on redelivery; {@code PROCESSED}/{@code SKIPPED} are not.
 */
@Service
public class AnalyticsInboxService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsInboxService.class);

    private final InboxEventRepository inboxEventRepository;
    private final AnalyticsProjectionService projectionService;

    public AnalyticsInboxService(
            InboxEventRepository inboxEventRepository,
            AnalyticsProjectionService projectionService) {
        this.inboxEventRepository = inboxEventRepository;
        this.projectionService = projectionService;
    }

    @Transactional
    public void process(DomainEventMessage event) {
        Optional<InboxEvent> existing = inboxEventRepository.findByEventId(event.eventId());
        InboxEvent inbox;
        if (existing.isPresent()) {
            inbox = existing.get();
            if ("PROCESSED".equals(inbox.getStatus()) || "SKIPPED".equals(inbox.getStatus())) {
                log.info("Duplicate analytics event skipped eventId={} status={}",
                        event.eventId(), inbox.getStatus());
                return;
            }
            if (!"FAILED".equals(inbox.getStatus()) && !"PENDING".equals(inbox.getStatus())) {
                log.info("Analytics event ignored eventId={} status={}", event.eventId(), inbox.getStatus());
                return;
            }
            log.info("Retrying analytics event eventId={} priorStatus={}", event.eventId(), inbox.getStatus());
            inbox.setStatus("PENDING");
            inbox.setPayload(event.payload() != null ? event.payload() : Map.of());
        } else {
            Instant now = Instant.now();
            inbox = new InboxEvent();
            inbox.setEventId(event.eventId());
            inbox.setEventType(event.eventType());
            inbox.setAggregateType(event.aggregateType());
            inbox.setAggregateId(event.aggregateId());
            inbox.setPayload(event.payload() != null ? event.payload() : Map.of());
            inbox.setReceivedAt(now);
            inbox.setStatus("PENDING");
            inboxEventRepository.save(inbox);
        }
        try {
            projectionService.apply(event);
            inbox.setStatus("PROCESSED");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
        } catch (RuntimeException ex) {
            inbox.setStatus("FAILED");
            inboxEventRepository.save(inbox);
            throw ex;
        }
    }
}
