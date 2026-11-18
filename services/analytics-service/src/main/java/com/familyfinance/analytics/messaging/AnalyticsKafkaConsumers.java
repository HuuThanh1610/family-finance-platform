package com.familyfinance.analytics.messaging;

import com.familyfinance.analytics.service.AnalyticsInboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "familyfinance.kafka.enabled", havingValue = "true")
public class AnalyticsKafkaConsumers {

    private final AnalyticsInboxService inboxService;
    private final ObjectMapper objectMapper;

    public AnalyticsKafkaConsumers(AnalyticsInboxService inboxService, ObjectMapper objectMapper) {
        this.inboxService = inboxService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = {
                    "${familyfinance.kafka.topics.payment-events:payment.provider.events}",
                    "${familyfinance.kafka.topics.wallet-ledger-events:wallet.ledger.events}",
                    "${familyfinance.kafka.topics.budget-events:budget.domain.events}",
                    "${familyfinance.kafka.topics.expense-events:expense.domain.events}"
            },
            groupId = "${spring.kafka.consumer.group-id:analytics-service}"
    )
    public void onDomainEvent(@Payload String raw) {
        try {
            DomainEventMessage event = objectMapper.readValue(raw, DomainEventMessage.class);
            inboxService.process(event);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid domain event payload", ex);
        }
    }
}