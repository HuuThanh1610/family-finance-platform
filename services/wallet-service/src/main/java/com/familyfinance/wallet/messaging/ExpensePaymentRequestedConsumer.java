package com.familyfinance.wallet.messaging;

import com.familyfinance.wallet.dto.ExpensePaymentCommand;
import com.familyfinance.wallet.entity.InboxEvent;
import com.familyfinance.wallet.exception.PermanentExpensePaymentException;
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
 * Idempotent consumer: ExpensePaymentRequested → EXPENSE_PAYMENT ledger debit.
 * Permanent failures emit ExpensePaymentFailed without posting.
 */
@Component
@ConditionalOnProperty(name = "familyfinance.kafka.enabled", havingValue = "true")
public class ExpensePaymentRequestedConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExpensePaymentRequestedConsumer.class);
    public static final String EVENT_TYPE = "ExpensePaymentRequested";

    private final InboxEventRepository inboxEventRepository;
    private final WalletApplicationService walletApplicationService;
    private final ObjectMapper objectMapper;

    public ExpensePaymentRequestedConsumer(
            InboxEventRepository inboxEventRepository,
            WalletApplicationService walletApplicationService,
            ObjectMapper objectMapper
    ) {
        this.inboxEventRepository = inboxEventRepository;
        this.walletApplicationService = walletApplicationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.expense-events:expense.domain.events}",
            groupId = "${spring.kafka.consumer.group-id:wallet-service}"
    )
    @Transactional
    public void onMessage(@Payload String raw) {
        DomainEventMessage event;
        try {
            event = objectMapper.readValue(raw, DomainEventMessage.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid ExpensePaymentRequested payload", ex);
        }
        if (!EVENT_TYPE.equals(event.eventType())) {
            return;
        }
        if (inboxEventRepository.existsByEventId(event.eventId())) {
            log.info("Duplicate ExpensePaymentRequested skipped eventId={}", event.eventId());
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

        ExpensePaymentCommand command = null;
        try {
            command = parseCommand(event);
            walletApplicationService.payExpense(command);
            inbox.setStatus("PROCESSED");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
        } catch (PermanentExpensePaymentException ex) {
            log.warn("Permanent expense payment failure expenseId={}: {} — {}",
                    command != null ? command.expenseId() : null, ex.getErrorCode(), ex.getMessage());
            if (command == null) {
                command = parseCommandLenient(event);
            }
            walletApplicationService.recordExpensePaymentFailure(command, ex.getErrorCode(), ex.getMessage());
            inbox.setStatus("PROCESSED");
            inbox.setProcessedAt(Instant.now());
            inboxEventRepository.save(inbox);
        } catch (RuntimeException ex) {
            inbox.setStatus("FAILED");
            inboxEventRepository.save(inbox);
            throw ex;
        }
    }

    private static ExpensePaymentCommand parseCommand(DomainEventMessage event) {
        Map<String, Object> p = event.payload() != null ? event.payload() : Map.of();
        try {
            UUID expenseId = UUID.fromString(String.valueOf(p.get("expenseId")));
            UUID familyId = UUID.fromString(String.valueOf(p.get("familyId")));
            UUID walletId = UUID.fromString(String.valueOf(p.get("walletId")));
            long amount = Long.parseLong(String.valueOf(p.get("amount")));
            String currency = p.get("currencyCode") != null ? String.valueOf(p.get("currencyCode")) : "VND";
            String referenceId = p.get("referenceId") != null
                    ? String.valueOf(p.get("referenceId"))
                    : WalletApplicationService.REF_EXPENSE_PREFIX + expenseId;
            String idempotencyKey = p.get("idempotencyKey") != null
                    ? String.valueOf(p.get("idempotencyKey"))
                    : WalletApplicationService.IDEMPOTENCY_EXPENSE_PREFIX + expenseId;
            return new ExpensePaymentCommand(
                    expenseId, familyId, walletId, amount, currency, referenceId, idempotencyKey);
        } catch (RuntimeException ex) {
            throw new PermanentExpensePaymentException("INVALID_PAYLOAD", ex.getMessage());
        }
    }

    private static ExpensePaymentCommand parseCommandLenient(DomainEventMessage event) {
        Map<String, Object> p = event.payload() != null ? event.payload() : Map.of();
        UUID expenseId = event.aggregateId();
        try {
            if (p.get("expenseId") != null) {
                expenseId = UUID.fromString(String.valueOf(p.get("expenseId")));
            }
        } catch (Exception ignored) {
            // keep aggregate
        }
        UUID familyId = null;
        UUID walletId = null;
        long amount = 0L;
        try {
            if (p.get("familyId") != null) {
                familyId = UUID.fromString(String.valueOf(p.get("familyId")));
            }
            if (p.get("walletId") != null) {
                walletId = UUID.fromString(String.valueOf(p.get("walletId")));
            }
            if (p.get("amount") != null) {
                amount = Long.parseLong(String.valueOf(p.get("amount")));
            }
        } catch (Exception ignored) {
            // best-effort
        }
        return new ExpensePaymentCommand(
                expenseId, familyId, walletId, amount, "VND",
                WalletApplicationService.REF_EXPENSE_PREFIX + expenseId,
                WalletApplicationService.IDEMPOTENCY_EXPENSE_PREFIX + expenseId);
    }
}
