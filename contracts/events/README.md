# Domain Events

Naming: `<context>.<aggregate>.<event>`

| Event | Producer | Consumers (planned) |
|-------|----------|---------------------|
| `wallet.ledger.posted` | wallet-service | analytics, notification |
| `payment.order.succeeded` | payment-service | wallet (credit), notification |
| `budget.allowance.approved` | budget-service | wallet, notification |
| `InventoryLow` (`household.domain.events`) | household-service outbox | notification (OWNER/PARENT via Family internal members) |
| `family.member.invited` | family-service | notification |

See `docs/events/KAFKA_CONTRACTS.md` for InventoryLow payload and recipient policy.

Payload schema stubs can be added as JSON Schema / Avro later.
Schema outbox/inbox tables đã có trong wallet/payment/budget.
