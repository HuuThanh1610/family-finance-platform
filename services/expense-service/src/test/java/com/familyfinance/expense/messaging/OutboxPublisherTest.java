package com.familyfinance.expense.messaging;

import com.familyfinance.expense.entity.OutboxEvent;
import com.familyfinance.expense.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock OutboxEventRepository outboxEventRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;

    OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(
                outboxEventRepository,
                kafkaTemplate,
                new ObjectMapper().findAndRegisterModules(),
                "expense.domain.events");
    }

    @Test
    void marksPublishedAfterAck() {
        OutboxEvent pending = sample("PENDING");
        when(outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(Set.of("PENDING", "FAILED")))
                .thenReturn(List.of(pending));
        when(kafkaTemplate.send(eq("expense.domain.events"), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPending();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void kafkaFailureLeavesRetryableFailed() {
        OutboxEvent pending = sample("PENDING");
        when(outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(Set.of("PENDING", "FAILED")))
                .thenReturn(List.of(pending));
        when(kafkaTemplate.send(eq("expense.domain.events"), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        publisher.publishPending();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getPublishedAt()).isNull();
    }

    private OutboxEvent sample(String status) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setEventType("ExpensePaymentRequested");
        event.setAggregateType("expense");
        event.setAggregateId(UUID.randomUUID());
        event.setPayload(Map.of("familyId", UUID.randomUUID().toString()));
        event.setCreatedAt(Instant.now());
        event.setStatus(status);
        return event;
    }
}
