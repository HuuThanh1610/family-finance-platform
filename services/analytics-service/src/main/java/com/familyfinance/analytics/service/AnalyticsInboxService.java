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
        if (inboxEventRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate analytics event skipped eventId={}", event.eventId());
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