package com.familyfinance.notification.service;

import com.familyfinance.notification.messaging.DomainEventMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEventMapperTest {

    @Test
    void mapsExpenseEvents() {
        UUID user = UUID.randomUUID();
        var draft = NotificationEventMapper.toDraft(new DomainEventMessage(
                UUID.randomUUID(),
                "ExpensePaymentRequested",
                "expense",
                UUID.randomUUID(),
                null,
                Instant.now(),
                Map.of(
                        "familyId", UUID.randomUUID().toString(),
                        "recipientUserId", user.toString(),
                        "amount", 50_000
                )
        ));
        assertThat(draft).isPresent();
        assertThat(draft.get().recipientUserId()).isEqualTo(user);
        assertThat(draft.get().templateKey()).isEqualTo("EXPENSE");
    }

    @Test
    void ignoresUnknownEventType() {
        var draft = NotificationEventMapper.toDraft(new DomainEventMessage(
                UUID.randomUUID(),
                "Unknown",
                "x",
                UUID.randomUUID(),
                null,
                Instant.now(),
                Map.of()
        ));
        assertThat(draft).isEmpty();
    }

    @Test
    void inventoryLowRequiresMembershipFanOutNotPayloadRecipient() {
        var draft = NotificationEventMapper.toDraft(new DomainEventMessage(
                UUID.randomUUID(),
                "InventoryLow",
                "inventory_item",
                UUID.randomUUID(),
                null,
                Instant.now(),
                Map.of(
                        "familyId", UUID.randomUUID().toString(),
                        "quantityOnHand", 1,
                        "reorderLevel", 5
                )
        ));
        assertThat(draft).isEmpty();
    }
}