package com.familyfinance.payment.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEventMessage(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        String correlationId,
        Instant occurredAt,
        Map<String, Object> payload
) {
}
