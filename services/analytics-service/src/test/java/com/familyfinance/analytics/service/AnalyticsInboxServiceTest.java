package com.familyfinance.analytics.service;

import com.familyfinance.analytics.entity.InboxEvent;
import com.familyfinance.analytics.messaging.DomainEventMessage;
import com.familyfinance.analytics.projection.AnalyticsProjectionService;
import com.familyfinance.analytics.repository.InboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsInboxServiceTest {

    @Mock InboxEventRepository inboxEventRepository;
    @Mock AnalyticsProjectionService projectionService;

    AnalyticsInboxService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsInboxService(inboxEventRepository, projectionService);
    }

    @Test
    void inboxAndProjectionWriteAtomicallyInOrder() {
        UUID eventId = UUID.randomUUID();
        DomainEventMessage event = message(eventId, "WalletCreditPosted");
        when(inboxEventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        when(inboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.process(event);

        InOrder order = inOrder(inboxEventRepository, projectionService);
        order.verify(inboxEventRepository).save(any(InboxEvent.class));
        order.verify(projectionService).apply(event);
        ArgumentCaptor<InboxEvent> captor = ArgumentCaptor.forClass(InboxEvent.class);
        verify(inboxEventRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getStatus()).isEqualTo("PROCESSED");
    }

    @Test
    void processedEventIsSkippedOnRedelivery() {
        UUID eventId = UUID.randomUUID();
        InboxEvent existing = new InboxEvent();
        existing.setEventId(eventId);
        existing.setStatus("PROCESSED");
        when(inboxEventRepository.findByEventId(eventId)).thenReturn(Optional.of(existing));

        service.process(message(eventId, "WalletCreditPosted"));

        verify(projectionService, never()).apply(any());
        verify(inboxEventRepository, never()).save(any());
    }

    @Test
    void failedEventIsRetriedOnRedelivery() {
        UUID eventId = UUID.randomUUID();
        InboxEvent existing = new InboxEvent();
        existing.setEventId(eventId);
        existing.setStatus("FAILED");
        existing.setEventType("WalletCreditPosted");
        existing.setAggregateType("ledger_transaction");
        existing.setAggregateId(UUID.randomUUID());
        when(inboxEventRepository.findByEventId(eventId)).thenReturn(Optional.of(existing));
        when(inboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DomainEventMessage event = message(eventId, "WalletCreditPosted");
        service.process(event);

        verify(projectionService).apply(event);
        assertThat(existing.getStatus()).isEqualTo("PROCESSED");
    }

    @Test
    void projectionFailureMarksInboxFailed() {
        UUID eventId = UUID.randomUUID();
        when(inboxEventRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        when(inboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalStateException("boom")).when(projectionService).apply(any());

        assertThatThrownBy(() -> service.process(message(eventId, "WalletDebitPosted")))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<InboxEvent> captor = ArgumentCaptor.forClass(InboxEvent.class);
        verify(inboxEventRepository, atLeast(2)).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getStatus()).isEqualTo("FAILED");
    }

    private static DomainEventMessage message(UUID eventId, String type) {
        return new DomainEventMessage(
                eventId,
                type,
                "ledger_transaction",
                UUID.randomUUID(),
                null,
                Instant.now(),
                Map.of("familyId", UUID.randomUUID().toString(), "amount", 1)
        );
    }
}
