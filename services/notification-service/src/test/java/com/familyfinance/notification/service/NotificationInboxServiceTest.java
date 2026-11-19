package com.familyfinance.notification.service;

import com.familyfinance.notification.client.FamilyMemberClient;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationInboxServiceTest {

    @Mock InboxEventRepository inboxEventRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock NotificationPreferenceService preferenceService;
    @Mock FamilyMemberClient familyMemberClient;

    NotificationInboxService service;

    @BeforeEach
    void setUp() {
        service = new NotificationInboxService(
                inboxEventRepository, notificationRepository, preferenceService, familyMemberClient);
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

    @Test
    void inventoryLowFansOutToOwnerAndParentOnly() {
        UUID eventId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID removedParent = UUID.randomUUID();

        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(familyMemberClient.listActiveMembers(familyId)).thenReturn(List.of(
                new FamilyMemberClient.InternalMember(owner, "OWNER", "ACTIVE"),
                new FamilyMemberClient.InternalMember(parent, "PARENT", "ACTIVE"),
                new FamilyMemberClient.InternalMember(child, "CHILD", "ACTIVE"),
                new FamilyMemberClient.InternalMember(removedParent, "PARENT", "REMOVED")
        ));
        when(preferenceService.isChannelEnabled(any(), eq("IN_APP"), eq("HOUSEHOLD"))).thenReturn(true);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.process(inventoryLow(eventId, familyId));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getRecipientUserId)
                .containsExactlyInAnyOrder(owner, parent);
        assertThat(captor.getAllValues())
                .allSatisfy(n -> assertThat(n.getFamilyId()).isEqualTo(familyId));
    }

    @Test
    void inventoryLowReplayDoesNotCreateDuplicateNotifications() {
        UUID eventId = UUID.randomUUID();
        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(true);

        service.process(inventoryLow(eventId, UUID.randomUUID()));

        verify(familyMemberClient, never()).listActiveMembers(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void inventoryLowDoesNotNotifyOtherFamilyMembers() {
        UUID eventId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(false);
        // Family API is scoped by familyId — only that family's members are returned
        when(familyMemberClient.listActiveMembers(familyId)).thenReturn(List.of(
                new FamilyMemberClient.InternalMember(UUID.randomUUID(), "OWNER", "ACTIVE")
        ));
        when(preferenceService.isChannelEnabled(any(), eq("IN_APP"), eq("HOUSEHOLD"))).thenReturn(true);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.process(inventoryLow(eventId, familyId));

        verify(familyMemberClient).listActiveMembers(familyId);
        verify(familyMemberClient, never()).listActiveMembers(argThat(id -> !familyId.equals(id)));
    }

    @Test
    void inventoryLowLookupFailureDoesNotClaimInbox() {
        UUID eventId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        when(inboxEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(familyMemberClient.listActiveMembers(familyId))
                .thenThrow(new FamilyMemberClient.FamilyLookupException("down", new RuntimeException("timeout")));

        assertThatThrownBy(() -> service.process(inventoryLow(eventId, familyId)))
                .isInstanceOf(FamilyMemberClient.FamilyLookupException.class);

        verify(inboxEventRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
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

    private DomainEventMessage inventoryLow(UUID eventId, UUID familyId) {
        return new DomainEventMessage(
                eventId,
                "InventoryLow",
                "inventory_item",
                UUID.randomUUID(),
                eventId.toString(),
                Instant.now(),
                Map.of(
                        "inventoryItemId", UUID.randomUUID().toString(),
                        "familyId", familyId.toString(),
                        "quantityOnHand", 2,
                        "reorderLevel", 5
                )
        );
    }
}
