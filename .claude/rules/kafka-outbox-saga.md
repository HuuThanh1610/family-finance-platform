---
paths:
  - "services/**/messaging/**/*.java"
  - "services/**/entity/OutboxEvent.java"
  - "services/**/entity/InboxEvent.java"
  - "services/**/repository/*Outbox*"
  - "services/**/repository/*Inbox*"
  - "docs/events/**"
  - "docs/workflows/**"
  - "contracts/events/**"
---

# Kafka / Outbox / Saga

Follow `docs/events/KAFKA_CONTRACTS.md` and existing `DomainEventMessage` envelope (`eventId`, `eventType`, `aggregate*`, `correlationId`, `occurredAt`, `payload`).

- Delivery is **at-least-once**. Consumers must be idempotent (Inbox / unique event id).
- Publish via transactional Outbox; set `PUBLISHED` only after broker send succeeds. Prefer short DB TX then relay—avoid long broker calls inside DB TX.
- Reuse topics/producers/consumers already in the service; do not duplicate contracts or publishers.
- Keep correlation/causation where workflows need them; honor ordering when correctness depends on it.
- Handle retries and poison/failed outbox explicitly (existing status fields).
- Event delivery ≠ business completion. Sagas need success, failure, retry, and recovery paths (see `docs/workflows/`).
- No distributed XA. Analytics must not treat request/intent events as completed financial postings.
