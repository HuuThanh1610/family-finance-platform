package com.familyfinance.wallet.messaging;

import com.familyfinance.wallet.dto.AllowanceDisbursementCommand;
import com.familyfinance.wallet.entity.InboxEvent;
import com.familyfinance.wallet.exception.PermanentAllowanceDisbursementException;
import com.familyfinance.wallet.repository.InboxEventRepository;
import com.familyfinance.wallet.service.WalletApplicationService;
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
 * Idempotent consumer: AllowanceDisbursementRequested → internal ledger transfer.
 * Permanent business failures emit AllowanceDisbursementFailed and acknowledge the inbox.
 */
@Component
@ConditionalOnProperty(name = "familyfinance.kafka.enabled", havingValue = "true")
public class AllowanceDisbursementRequestedConsumer {

    private static final Logger log = LoggerFactory.getLogger(AllowanceDisbursementRequestedConsumer.class);
    public static final String EVENT_TYPE = "AllowanceDisbursementRequested";

    private final InboxEventRepository inboxEventRepository;
    private final WalletApplicationService walletApplicationService;
    private final ObjectMapper objectMapper;

    public AllowanceDisbursementRequestedConsumer(
            InboxEventRepository inboxEventRepository,
            WalletApplicationService walletApplicationService,
            ObjectMapper objectMapper
    ) {
        this.inboxEventRepository = inboxEventRepository;
        this.walletApplicationService = walletApplicationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.budget-events:budget.domain.events}",
            groupId = "${spring.kafka.consumer.group-id:wallet-service}"
    )
    @Transactional
    public void onMessage(@Payload String raw) {
        DomainEventMessage event;
        try {
            event = objectMapper.readValue(raw, DomainEventMessage.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid AllowanceDisbursementRequested payload", ex);
        }
        if (!EVENT_TYPE.equals(event.eventType())) {
            return;
        }
        if (inboxEventRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate AllowanceDisbursementRequested skipped eventId={}", event.eventId());
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

        AllowanceDisbursementCommand command = null;
        try {
            command = parseCommand(event);
            walletApplicationService.disburseAllowance(command);
            inbox.setStatus("PROCESSED");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
        } catch (PermanentAllowanceDisbursementException ex) {
            log.warn("Permanent allowance disbursement failure requestId={}: {} — {}",
                    command != null ? command.allowanceRequestId() : null, ex.getErrorCode(), ex.getMessage());
            if (command == null) {
                command = parseCommandLenient(event);
            }
            walletApplicationService.recordAllowanceDisbursementFailure(command, ex.getErrorCode(), ex.getMessage());
            inbox.setStatus("PROCESSED");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
        } catch (RuntimeException ex) {
            inbox.setStatus("FAILED");
            inboxEventRepository.save(inbox);
            throw ex;
        }
    }

    private static AllowanceDisbursementCommand parseCommand(DomainEventMessage event) {
        Map<String, Object> p = event.payload() != null ? event.payload() : Map.of();
        try {
            UUID requestId = uuidRequired(p, "requestId");
            UUID familyId = uuidRequired(p, "familyId");
            UUID requesterMemberId = uuidRequired(p, "requesterMemberId");
            long amount = Long.parseLong(String.valueOf(p.get("amount")));
            String currency = p.get("currencyCode") != null ? String.valueOf(p.get("currencyCode")) : "VND";
            return new AllowanceDisbursementCommand(requestId, familyId, requesterMemberId, amount, currency);
        } catch (RuntimeException ex) {
            throw new PermanentAllowanceDisbursementException("INVALID_PAYLOAD", ex.getMessage());
        }
    }

    private static AllowanceDisbursementCommand parseCommandLenient(DomainEventMessage event) {
        Map<String, Object> p = event.payload() != null ? event.payload() : Map.of();
        UUID requestId = event.aggregateId();
        try {
            if (p.get("requestId") != null) {
                requestId = UUID.fromString(String.valueOf(p.get("requestId")));
            }
        } catch (Exception ignored) {
            // keep aggregateId
        }
        UUID familyId = null;
        UUID memberId = null;
        long amount = 0L;
        try {
            if (p.get("familyId") != null) {
                familyId = UUID.fromString(String.valueOf(p.get("familyId")));
            }
            if (p.get("requesterMemberId") != null) {
                memberId = UUID.fromString(String.valueOf(p.get("requesterMemberId")));
            }
            if (p.get("amount") != null) {
                amount = Long.parseLong(String.valueOf(p.get("amount")));
            }
        } catch (Exception ignored) {
            // best-effort failure payload
        }
        return new AllowanceDisbursementCommand(requestId, familyId, memberId, amount, "VND");
    }

    private static UUID uuidRequired(Map<String, Object> payload, String key) {
        Object raw = payload.get(key);
        if (raw == null) {
            throw new IllegalArgumentException("Missing " + key);
        }
        return UUID.fromString(String.valueOf(raw));
    }
}
