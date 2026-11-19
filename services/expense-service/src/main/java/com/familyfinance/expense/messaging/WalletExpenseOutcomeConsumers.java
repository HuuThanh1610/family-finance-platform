package com.familyfinance.expense.messaging;

import com.familyfinance.expense.entity.InboxEvent;
import com.familyfinance.expense.exception.BusinessException;
import com.familyfinance.expense.exception.ResourceNotFoundException;
import com.familyfinance.expense.repository.InboxEventRepository;
import com.familyfinance.expense.service.ExpenseService;
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
 * Closes expense settlement from Wallet outcomes.
 * Success: WalletDebitPosted (EXPENSE_PAYMENT). Failure: ExpensePaymentFailed.
 */
@Component
@ConditionalOnProperty(name = "familyfinance.kafka.enabled", havingValue = "true")
public class WalletExpenseOutcomeConsumers {

    private static final Logger log = LoggerFactory.getLogger(WalletExpenseOutcomeConsumers.class);

    public static final String EVENT_DEBIT_POSTED = "WalletDebitPosted";
    public static final String EVENT_PAYMENT_FAILED = "ExpensePaymentFailed";

    private final InboxEventRepository inboxEventRepository;
    private final ExpenseService expenseService;
    private final ObjectMapper objectMapper;

    public WalletExpenseOutcomeConsumers(
            InboxEventRepository inboxEventRepository,
            ExpenseService expenseService,
            ObjectMapper objectMapper
    ) {
        this.inboxEventRepository = inboxEventRepository;
        this.expenseService = expenseService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.wallet-ledger-events:wallet.ledger.events}",
            groupId = "${spring.kafka.consumer.group-id:expense-service}"
    )
    @Transactional
    public void onLedger(@Payload String raw) {
        DomainEventMessage event = read(raw);
        if (!EVENT_DEBIT_POSTED.equals(event.eventType())) {
            return;
        }
        Map<String, Object> payload = event.payload() != null ? event.payload() : Map.of();
        if (!"EXPENSE_PAYMENT".equals(String.valueOf(payload.get("transactionType")))) {
            return;
        }
        process(event, () -> {
            UUID expenseId = expenseIdFromReference(String.valueOf(payload.get("referenceId")));
            UUID familyId = UUID.fromString(String.valueOf(payload.get("familyId")));
            UUID walletId = UUID.fromString(String.valueOf(payload.get("walletId")));
            long amount = Long.parseLong(String.valueOf(payload.get("amount")));
            UUID transactionId = payload.get("transactionId") != null
                    ? UUID.fromString(String.valueOf(payload.get("transactionId")))
                    : null;
            expenseService.applyWalletDebitPosted(
                    expenseId,
                    familyId,
                    walletId,
                    amount,
                    String.valueOf(payload.get("referenceId")),
                    String.valueOf(payload.get("transactionType")),
                    transactionId
            );
        });
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.wallet-domain-events:wallet.domain.events}",
            groupId = "${spring.kafka.consumer.group-id:expense-service}"
    )
    @Transactional
    public void onDomain(@Payload String raw) {
        DomainEventMessage event = read(raw);
        if (!EVENT_PAYMENT_FAILED.equals(event.eventType())) {
            return;
        }
        process(event, () -> {
            Map<String, Object> payload = event.payload() != null ? event.payload() : Map.of();
            UUID expenseId = UUID.fromString(String.valueOf(payload.get("expenseId")));
            String errorCode = String.valueOf(payload.getOrDefault("errorCode", "PAYMENT_FAILED"));
            expenseService.applyPaymentFailed(expenseId, errorCode);
        });
    }

    private void process(DomainEventMessage event, Runnable action) {
        if (inboxEventRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate expense outcome skipped eventId={}", event.eventId());
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
            action.run();
            inbox.setStatus("PROCESSED");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
        } catch (BusinessException | ResourceNotFoundException | IllegalArgumentException ex) {
            log.warn("Permanent expense outcome skip eventId={}: {}", event.eventId(), ex.toString());
            inbox.setStatus("SKIPPED");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
        } catch (RuntimeException ex) {
            inbox.setStatus("FAILED");
            inboxEventRepository.save(inbox);
            throw ex;
        }
    }

    private DomainEventMessage read(String raw) {
        try {
            return objectMapper.readValue(raw, DomainEventMessage.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid wallet outcome payload", ex);
        }
    }

    static UUID expenseIdFromReference(String referenceId) {
        if (referenceId == null || !referenceId.startsWith(ExpenseService.REF_PREFIX)) {
            throw new IllegalArgumentException("Invalid expense referenceId: " + referenceId);
        }
        return UUID.fromString(referenceId.substring(ExpenseService.REF_PREFIX.length()));
    }
}
