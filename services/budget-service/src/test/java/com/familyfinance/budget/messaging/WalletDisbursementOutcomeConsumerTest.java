package com.familyfinance.budget.messaging;

import com.familyfinance.budget.repository.InboxEventRepository;
import com.familyfinance.budget.service.AllowanceRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletDisbursementOutcomeConsumerTest {

    @Mock InboxEventRepository inboxEventRepository;
    @Mock AllowanceRequestService allowanceRequestService;

    WalletDisbursementOutcomeConsumer consumer;
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        consumer = new WalletDisbursementOutcomeConsumer(
                inboxEventRepository, allowanceRequestService, objectMapper);
    }

    @Test
    void completedAppliesDisbursed() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(inboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onMessage(objectMapper.writeValueAsString(new DomainEventMessage(
                eventId,
                WalletDisbursementOutcomeConsumer.EVENT_COMPLETED,
                "allowance_request",
                requestId,
                null,
                Instant.now(),
                Map.of("requestId", requestId.toString())
        )));

        verify(allowanceRequestService).applyDisbursementCompleted(requestId);
    }

    @Test
    void failedAppliesDisbursementFailed() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(inboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onMessage(objectMapper.writeValueAsString(new DomainEventMessage(
                eventId,
                WalletDisbursementOutcomeConsumer.EVENT_FAILED,
                "allowance_request",
                requestId,
                null,
                Instant.now(),
                Map.of("requestId", requestId.toString(), "errorCode", "INSUFFICIENT_FUNDS")
        )));

        verify(allowanceRequestService).applyDisbursementFailed(requestId, "INSUFFICIENT_FUNDS");
    }

    @Test
    void duplicateEventIdSkipped() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(true);

        consumer.onMessage(objectMapper.writeValueAsString(new DomainEventMessage(
                eventId,
                WalletDisbursementOutcomeConsumer.EVENT_COMPLETED,
                "allowance_request",
                UUID.randomUUID(),
                null,
                Instant.now(),
                Map.of("requestId", UUID.randomUUID().toString())
        )));

        verify(allowanceRequestService, never()).applyDisbursementCompleted(any());
        verify(inboxEventRepository, never()).save(any());
    }
}
