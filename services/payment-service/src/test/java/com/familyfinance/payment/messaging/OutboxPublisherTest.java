package com.familyfinance.payment.messaging;

import com.familyfinance.payment.entity.OutboxEvent;
import com.familyfinance.payment.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(
                outboxEventRepository, kafkaTemplate, new ObjectMapper(), "payment.provider.events");
    }

    @Test
    void publishPending_retriesFailedEvents() throws Exception {
        OutboxEvent failed = new OutboxEvent();
        failed.setId(UUID.randomUUID());
        failed.setEventType("PaymentProviderPaid");
        failed.setAggregateType("payment_order");
        failed.setAggregateId(UUID.randomUUID());
        failed.setPayload(Map.of("walletId", UUID.randomUUID().toString()));
        failed.setStatus("FAILED");

        when(outboxEventRepository.findTop50ByStatusInOrderByCreatedAtAsc(Set.of("PENDING", "FAILED")))
                .thenReturn(List.of(failed));
        when(kafkaTemplate.send(eq("payment.provider.events"), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPending();

        verify(kafkaTemplate).send(eq("payment.provider.events"), any(), any());
        verify(outboxEventRepository).save(failed);
    }
}