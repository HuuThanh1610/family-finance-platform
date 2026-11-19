package com.familyfinance.expense.messaging;

import com.familyfinance.expense.entity.OutboxEvent;
import com.familyfinance.expense.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "familyfinance.kafka.enabled", havingValue = "true")
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String expenseEventsTopic;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${familyfinance.kafka.topics.expense-events:expense.domain.events}") String expenseEventsTopic
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.expenseEventsTopic = expenseEventsTopic;
    }

    @Scheduled(fixedDelayString = "${familyfinance.outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(
                Set.of("PENDING", "FAILED"));
        for (OutboxEvent event : batch) {
            try {
                DomainEventMessage message = new DomainEventMessage(
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getId().toString(),
                        event.getCreatedAt(),
                        event.getPayload()
                );
                String key = event.getAggregateId().toString();
                if (event.getPayload() != null && event.getPayload().get("familyId") != null) {
                    key = event.getPayload().get("familyId").toString();
                }
                kafkaTemplate.send(expenseEventsTopic, key, objectMapper.writeValueAsString(message)).get();
                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
            } catch (Exception ex) {
                log.warn("Expense outbox publish failed id={}: {}", event.getId(), ex.toString());
                event.setStatus("FAILED");
                outboxEventRepository.save(event);
            }
        }
    }
}
