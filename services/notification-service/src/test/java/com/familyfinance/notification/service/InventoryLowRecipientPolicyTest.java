package com.familyfinance.notification.service;

import com.familyfinance.notification.client.FamilyMemberClient.InternalMember;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryLowRecipientPolicyTest {

    @Test
    void includesOnlyActiveOwnerAndParent() {
        UUID owner = UUID.randomUUID();
        UUID parent = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        UUID viewer = UUID.randomUUID();
        UUID left = UUID.randomUUID();

        List<UUID> recipients = InventoryLowRecipientPolicy.resolveRecipients(List.of(
                new InternalMember(owner, "OWNER", "ACTIVE"),
                new InternalMember(parent, "PARENT", "ACTIVE"),
                new InternalMember(child, "CHILD", "ACTIVE"),
                new InternalMember(viewer, "VIEWER", "ACTIVE"),
                new InternalMember(left, "PARENT", "LEFT"),
                new InternalMember(UUID.randomUUID(), "OWNER", "REMOVED")
        ));

        assertThat(recipients).containsExactly(owner, parent);
    }

    @Test
    void deduplicatesSameUserProfile() {
        UUID user = UUID.randomUUID();
        List<UUID> recipients = InventoryLowRecipientPolicy.resolveRecipients(List.of(
                new InternalMember(user, "OWNER", "ACTIVE"),
                new InternalMember(user, "OWNER", "ACTIVE")
        ));
        assertThat(recipients).containsExactly(user);
    }
}
