# Domain Events

Naming: `<context>.<aggregate>.<event>`

| Event | Producer | Consumers (planned) |
|-------|----------|---------------------|
| `wallet.ledger.posted` | wallet-service | analytics, notification |
| `payment.order.succeeded` | payment-service | wallet (credit), notification |
| `budget.allowance.approved` | budget-service | wallet, notification |
| `household.inventory.low_stock` | household-service | notification |
| `family.member.invited` | family-service | notification |

Payload schema stubs can be added as JSON Schema / Avro later.
Schema outbox/inbox tables đã có trong wallet/payment/budget.
