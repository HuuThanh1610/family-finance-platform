package com.familyfinance.notification.service;

import com.familyfinance.notification.messaging.DomainEventMessage;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class NotificationEventMapper {

    private NotificationEventMapper() {
    }

    public static boolean isNotifiable(String eventType) {
        if (eventType == null) {
            return false;
        }
        return "PaymentProviderPaid".equals(eventType)
                || "WalletCreditPosted".equals(eventType)
                || "FamilyInvitation".equals(eventType)
                || "InventoryLow".equals(eventType)
                || eventType.startsWith("Allowance")
                || eventType.startsWith("Expense");
    }

    public static Optional<NotificationDraft> toDraft(DomainEventMessage event) {
        if (!isNotifiable(event.eventType())) {
            return Optional.empty();
        }
        // InventoryLow recipients are resolved via Family membership (OWNER/PARENT), not payload.
        if ("InventoryLow".equals(event.eventType())) {
            return Optional.empty();
        }
        Map<String, Object> payload = event.payload() != null ? event.payload() : Map.of();
        UUID familyId = uuidFromPayload(payload, "familyId");
        UUID recipient = uuidFromPayload(payload, "recipientUserId", "userProfileId", "inviteeUserId");
        if (recipient == null) {
            recipient = uuidFromPayload(payload, "requesterMemberId");
        }
        if (recipient == null) {
            return Optional.empty();
        }

        return Optional.of(draftForRecipient(event, recipient, familyId));
    }

    public static NotificationDraft draftForRecipient(DomainEventMessage event, UUID recipient, UUID familyId) {
        Map<String, Object> payload = event.payload() != null ? event.payload() : Map.of();
        return new NotificationDraft(
                recipient,
                familyId,
                templateKeyFor(event.eventType()),
                "IN_APP",
                titleFor(event.eventType(), payload),
                bodyFor(event.eventType(), payload, event.aggregateId()),
                payload
        );
    }

    private static String templateKeyFor(String eventType) {
        if (eventType.startsWith("Allowance")) {
            return "ALLOWANCE";
        }
        if (eventType.startsWith("Expense")) {
            return "EXPENSE";
        }
        return switch (eventType) {
            case "PaymentProviderPaid", "WalletCreditPosted" -> "PAYMENT";
            case "FamilyInvitation" -> "FAMILY_INVITATION";
            case "InventoryLow" -> "INVENTORY_LOW";
            default -> "SYSTEM";
        };
    }

    private static String titleFor(String eventType, Map<String, Object> payload) {
        return switch (eventType) {
            case "PaymentProviderPaid" -> "Thanh toán nạp ví";
            case "WalletCreditPosted" -> "Số dư ví đã được cập nhật";
            case "FamilyInvitation" -> "Lời mời gia đình";
            case "InventoryLow" -> "Tồn kho thấp";
            case "AllowanceDisbursementRequested" -> "Yêu cầu tiền tiêu vặt được duyệt";
            case "ExpensePaymentRequested" -> "Chi tiêu chờ thanh toán";
            default -> eventType.replace('_', ' ');
        };
    }

    private static String bodyFor(String eventType, Map<String, Object> payload, UUID aggregateId) {
        Object amount = payload.get("amount");
        return switch (eventType) {
            case "PaymentProviderPaid", "WalletCreditPosted" ->
                    "Giao dịch " + formatAmount(amount) + " VND (ref " + aggregateId + ").";
            case "FamilyInvitation" ->
                    "Bạn được mời tham gia gia đình (mã " + aggregateId + ").";
            case "InventoryLow" ->
                    "Sản phẩm cần đặt thêm — tồn hiện tại " + payload.getOrDefault("quantityOnHand", "?") + ".";
            case "AllowanceDisbursementRequested" ->
                    "Yêu cầu " + formatAmount(amount) + " VND: " + payload.getOrDefault("reason", "");
            case "ExpensePaymentRequested" ->
                    "Chi " + formatAmount(amount) + " VND đang chờ trừ ví.";
            default -> "Sự kiện " + eventType;
        };
    }

    private static String formatAmount(Object amount) {
        return amount == null ? "0" : String.valueOf(amount);
    }

  static UUID uuidFromPayload(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object raw = payload.get(key);
            if (raw == null) {
                continue;
            }
            try {
                return UUID.fromString(String.valueOf(raw));
            } catch (IllegalArgumentException ignored) {
                // not a UUID — try next key
            }
        }
        return null;
    }

    public record NotificationDraft(
            UUID recipientUserId,
            UUID familyId,
            String templateKey,
            String channel,
            String title,
            String body,
            Map<String, Object> payload
    ) {
    }
}