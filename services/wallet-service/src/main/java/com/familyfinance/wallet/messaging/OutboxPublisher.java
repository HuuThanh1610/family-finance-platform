package com.familyfinance.wallet.messaging;

import com.familyfinance.wallet.entity.OutboxEvent;
import com.familyfinance.wallet.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polling outbox publisher. Compatible with future Debezium CDC replacement.
 */
@Component
@ConditionalOnProperty(name = "familyfinance.kafka.enabled", havingValue = "true")
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${familyfinance.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(
                java.util.Set.of("PENDING", "FAILED"));
        for (OutboxEvent event : batch) {
            try {
                String topic = topicFor(event.getEventType());
                DomainEventMessage message = new DomainEventMessage(
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        correlationId(event),
                        event.getCreatedAt(),
                        event.getPayload()
                );
                String key = event.getAggregateId().toString();
                if (event.getPayload() != null && event.getPayload().get("familyId") != null) {
                    key = event.getPayload().get("familyId").toString();
                }
                String body = objectMapper.writeValueAsString(message);
                kafkaTemplate.send(topic, key, body).get();
                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
            } catch (Exception ex) {
                log.warn("Outbox publish failed id={} type={}: {}", event.getId(), event.getEventType(), ex.toString());
                event.setStatus("FAILED");
                outboxEventRepository.save(event);
            }
        }
    }

    private static String topicFor(String eventType) {
        return switch (eventType) {
            case "WalletCreditPosted", "WalletDebitPosted", "WalletTransferPosted",
                 "wallet.ledger.posted" -> "wallet.ledger.events";
            case "AllowanceDisbursementCompleted", "AllowanceDisbursementFailed" -> "wallet.domain.events";
            default -> "wallet.domain.events";
        };
    }

    private static String correlationId(OutboxEvent event) {
        Object fromPayload = event.getPayload() != null ? event.getPayload().get("correlationId") : null;
        return fromPayload != null ? fromPayload.toString() : event.getId().toString();
    }
}
