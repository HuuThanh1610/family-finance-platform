# Domain Events

Naming: `<context>.<aggregate>.<event>`

| Event | Producer | Consumers (planned) |
|-------|----------|---------------------|
| `wallet.ledger.posted` | wallet-service | analytics, notification |
| `payment.order.succeeded` | payment-service | wallet (credit), notification |
| `AllowanceDisbursementRequested` (`budget.domain.events`) | budget-service outbox | wallet (FAMILY_POOL→MEMBER_WALLET) |
| `AllowanceDisbursementCompleted` / `Failed` (`wallet.domain.events`) | wallet-service outbox | budget (DISBURSED / DISBURSEMENT_FAILED) |
| `InventoryLow` (`household.domain.events`) | household-service outbox | notification (OWNER/PARENT via Family internal members) |
| `family.member.invited` | family-service | notification |

See `docs/events/KAFKA_CONTRACTS.md` for InventoryLow and Allowance disbursement saga contracts.

Payload schema stubs can be added as JSON Schema / Avro later.
Schema outbox/inbox tables đã có trong wallet/payment/budget.
