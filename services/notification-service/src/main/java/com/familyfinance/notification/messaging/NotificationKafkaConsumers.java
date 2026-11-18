package com.familyfinance.notification.messaging;

import com.familyfinance.notification.service.NotificationInboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "familyfinance.kafka.enabled", havingValue = "true")
public class NotificationKafkaConsumers {

    private final NotificationInboxService inboxService;
    private final ObjectMapper objectMapper;

    public NotificationKafkaConsumers(NotificationInboxService inboxService, ObjectMapper objectMapper) {
        this.inboxService = inboxService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.payment-events:payment.provider.events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}"
    )
    public void onPayment(@Payload String raw) {
        inboxService.process(read(raw));
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.wallet-ledger-events:wallet.ledger.events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}"
    )
    public void onWalletLedger(@Payload String raw) {
        inboxService.process(read(raw));
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.budget-events:budget.domain.events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}"
    )
    public void onBudget(@Payload String raw) {
        inboxService.process(read(raw));
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.expense-events:expense.domain.events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}"
    )
    public void onExpense(@Payload String raw) {
        inboxService.process(read(raw));
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.household-events:household.domain.events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}"
    )
    public void onHousehold(@Payload String raw) {
        inboxService.process(read(raw));
    }

    @KafkaListener(
            topics = "${familyfinance.kafka.topics.family-events:family.domain.events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}"
    )
    public void onFamily(@Payload String raw) {
        inboxService.process(read(raw));
    }

    private DomainEventMessage read(String raw) {
        try {
            return objectMapper.readValue(raw, DomainEventMessage.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid domain event payload", ex);
        }
    }
}