package com.familyfinance.notification.service;

import com.familyfinance.notification.entity.Notification;
import com.familyfinance.notification.messaging.DomainEventMessage;
import com.familyfinance.notification.repository.InboxEventRepository;
import com.familyfinance.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationInboxServiceTest {

    @Mock InboxEventRepository inboxEventRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock NotificationPreferenceService preferenceService;

    NotificationInboxService service;

    @BeforeEach
    void setUp() {
        service = new NotificationInboxService(inboxEventRepository, notificationRepository, preferenceService);
    }

    @Test
    void skipsDuplicateEventId() {
        UUID eventId = UUID.randomUUID();
        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(true);

        service.process(sampleEvent(eventId));

        verify(notificationRepository, never()).save(any());
        verify(inboxEventRepository, never()).save(any());
    }

    @Test
    void createsInAppNotificationFromAllowanceEvent() {
        UUID eventId = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(preferenceService.isChannelEnabled(any(), eq("IN_APP"), any())).thenReturn(true);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DomainEventMessage event = new DomainEventMessage(
                eventId,
                "AllowanceDisbursementRequested",
                "allowance_request",
                UUID.randomUUID(),
                "corr",
                Instant.now(),
                Map.of(
                        "familyId", UUID.randomUUID().toString(),
                        "requesterMemberId", recipient.toString(),
                        "amount", 150_000,
                        "reason", "Sách"
                )
        );

        service.process(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipientUserId()).isEqualTo(recipient);
        assertThat(captor.getValue().getTitle()).contains("tiêu vặt");
    }

    private DomainEventMessage sampleEvent(UUID eventId) {
        return new DomainEventMessage(
                eventId,
                "WalletCreditPosted",
                "ledger_transaction",
                UUID.randomUUID(),
                null,
                Instant.now(),
                Map.of()
        );
    }
}