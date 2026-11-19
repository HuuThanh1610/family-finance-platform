package com.familyfinance.budget.messaging;

import com.familyfinance.budget.entity.InboxEvent;
import com.familyfinance.budget.exception.InvalidAllowanceStateException;
import com.familyfinance.budget.exception.ResourceNotFoundException;
import com.familyfinance.budget.repository.InboxEventRepository;
import com.familyfinance.budget.service.AllowanceRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Closes the allowance saga from Wallet domain outcomes.
 * Success → DISBURSED; permanent failure → DISBURSEMENT_FAILED.
 */
@Component
@ConditionalOnProperty(name = "familyfinance.kafka.enabled", havingValue = "true")
public class WalletDisbursementOutcomeConsumer {

    private static final Logger log = LoggerFactory.getLogger(WalletDisbursementOutcomeConsumer.class);

    public static final String EVENT_COMPLETED = "AllowanceDisbursementCompleted";
    public static final String EVENT_FAILED = "AllowanceDisbursementFailed";

    private final InboxEventRepository inboxEventRepository;
    private final AllowanceRequestService allowanceRequestService;
    private final ObjectMapper objectMapper;

    public WalletDisbursementOutcomeConsumer(
            InboxEventRepository inboxEventRepository,
            AllowanceRequestService allowanceRequestService,
            ObjectMapper objectMapper
    ) {
        this.inboxEventRepository = inboxEventRepository;
        this.allowanceRequestService = allowanceRequestService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.wallet-domain-events:wallet.domain.events}",
            groupId = "${spring.kafka.consumer.group-id:budget-service}"
    )
    @Transactional
    public void onMessage(@Payload String raw) {
        DomainEventMessage event;
        try {
            event = objectMapper.readValue(raw, DomainEventMessage.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid wallet domain event payload", ex);
        }
        if (!EVENT_COMPLETED.equals(event.eventType()) && !EVENT_FAILED.equals(event.eventType())) {
            return;
        }
        if (inboxEventRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate wallet outcome skipped eventId={}", event.eventId());
            return;
        }

        InboxEvent inbox = new InboxEvent();
        inbox.setEventId(event.eventId());
        inbox.setEventType(event.eventType());
        inbox.setAggregateType(event.aggregateType());
        inbox.setAggregateId(event.aggregateId());
        inbox.setPayload(event.payload() != null ? event.payload() : Map.of());
        inbox.setReceivedAt(Instant.now());
        inbox.setStatus("PENDING");
        inboxEventRepository.save(inbox);

        try {
            UUID requestId = resolveRequestId(event);
            if (EVENT_COMPLETED.equals(event.eventType())) {
                allowanceRequestService.applyDisbursementCompleted(requestId);
            } else {
                String errorCode = String.valueOf(event.payload() != null
                        ? event.payload().getOrDefault("errorCode", "UNKNOWN")
                        : "UNKNOWN");
                allowanceRequestService.applyDisbursementFailed(requestId, errorCode);
            }
            inbox.setStatus("PROCESSED");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
        } catch (ResourceNotFoundException | InvalidAllowanceStateException | IllegalArgumentException ex) {
            log.warn("Permanent allowance outcome handling failure eventId={}: {}", event.eventId(), ex.toString());
            inbox.setStatus("SKIPPED");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
        } catch (RuntimeException ex) {
            inbox.setStatus("FAILED");
            inboxEventRepository.save(inbox);
            throw ex;
        }
    }

    private static UUID resolveRequestId(DomainEventMessage event) {
        Map<String, Object> payload = event.payload() != null ? event.payload() : Map.of();
        Object raw = payload.get("requestId");
        if (raw == null) {
            raw = payload.get("allowanceRequestId");
        }
        if (raw != null) {
            return UUID.fromString(String.valueOf(raw));
        }
        if ("allowance_request".equals(event.aggregateType()) && event.aggregateId() != null) {
            return event.aggregateId();
        }
        throw new IllegalArgumentException("Missing allowance requestId in wallet outcome");
    }
}
