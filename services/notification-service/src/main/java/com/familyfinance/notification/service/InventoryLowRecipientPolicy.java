package com.familyfinance.notification.service;

import com.familyfinance.notification.client.FamilyMemberClient.InternalMember;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Inventory low-stock alerts go to active household managers only: OWNER and PARENT.
 * CHILD / VIEWER / inactive members are excluded. Recipient = userProfileId.
 */
public final class InventoryLowRecipientPolicy {

    private static final Set<String> ALERT_ROLES = Set.of("OWNER", "PARENT");

    private InventoryLowRecipientPolicy() {
    }

    public static List<UUID> resolveRecipients(List<InternalMember> members) {
        LinkedHashSet<UUID> recipients = new LinkedHashSet<>();
        if (members == null) {
            return List.of();
        }
        for (InternalMember member : members) {
            if (member == null || member.userProfileId() == null) {
                continue;
            }
            if (!"ACTIVE".equalsIgnoreCase(member.status())) {
                continue;
            }
            if (!ALERT_ROLES.contains(member.roleCode())) {
                continue;
            }
            recipients.add(member.userProfileId());
        }
        return List.copyOf(recipients);
    }
}
