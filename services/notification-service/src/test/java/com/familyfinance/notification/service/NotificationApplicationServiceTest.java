package com.familyfinance.notification.service;

import com.familyfinance.notification.repository.InboxEventRepository;
import com.familyfinance.notification.repository.NotificationPreferenceRepository;
import com.familyfinance.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationPreferenceRepository preferenceRepository;
    @Mock InboxEventRepository inboxEventRepository;
    @InjectMocks NotificationApplicationService service;

    @Test
    void ingestEventIsIdempotent() {
        UUID eventId = UUID.randomUUID();
        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(true);

        service.ingestEvent(eventId, "WalletCreditPosted", "ledger_transaction", UUID.randomUUID(),
                Map.of("familyId", UUID.randomUUID().toString()), UUID.randomUUID(), "OK", "credited");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void ingestEventCreatesNotificationOnce() {
        UUID eventId = UUID.randomUUID();
        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(inboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ingestEvent(eventId, "InventoryLow", "inventory_item", UUID.randomUUID(),
                Map.of("familyId", UUID.randomUUID().toString()), UUID.randomUUID(), "Low stock", "item low");

        verify(notificationRepository, times(1)).save(any());
        verify(inboxEventRepository, times(2)).save(any());
    }
}
