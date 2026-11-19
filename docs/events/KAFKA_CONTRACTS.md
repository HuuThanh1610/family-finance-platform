# Kafka Event Contracts

Delivery: **at-least-once**. Consumers must be idempotent via `inbox_events.event_id` UNIQUE.

## Topics

| Topic | Producers | Consumers | Key |
|-------|-----------|-----------|-----|
| `payment.provider.events` | payment-service outbox | wallet-service | `paymentOrderId` / walletId |
| `wallet.ledger.events` | wallet-service outbox | payment, notification, analytics | `walletId` or transaction aggregate |
| `wallet.domain.events` | wallet-service | budget, analytics, notification | familyId / allowance requestId |
| `family.domain.events` | family-service | notification, profile | familyId |
| `budget.domain.events` | budget-service | wallet, notification | familyId |
| `expense.domain.events` | expense-service | wallet, notification | familyId |
| `household.domain.events` | household-service | notification | familyId |

## Envelope

```json
{
  "eventId": "uuid",
  "eventType": "PaymentProviderPaid",
  "aggregateType": "payment_order",
  "aggregateId": "uuid",
  "correlationId": "string",
  "occurredAt": "2026-11-18T00:00:00Z",
  "payload": {}
}
```

## PaymentProviderPaid

```json
{
  "paymentOrderId": "uuid",
  "familyId": "uuid",
  "walletId": "uuid",
  "amount": 100000,
  "currencyCode": "VND",
  "providerCode": "MOMO_SANDBOX",
  "providerTxnId": "string"
}
```

Wallet credits with idempotency key `pay-credit:{paymentOrderId}`.

## WalletCreditPosted

Emitted after ledger posts `PAYMENT_TOPUP`. Payment Service transitions `CREDIT_PENDING` → `COMPLETED`.

## Allowance disbursement saga

Sequence: approve (Budget) → `AllowanceDisbursementRequested` → Wallet ledger transfer →
`AllowanceDisbursementCompleted` | `AllowanceDisbursementFailed` → Budget `DISBURSED` | `DISBURSEMENT_FAILED`.

**Approval ≠ disbursement.** Budget stays `APPROVED_PENDING_DISBURSEMENT` until a Wallet outcome event is applied.

### AllowanceDisbursementRequested

| Field | Value |
|-------|--------|
| Topic | `budget.domain.events` |
| Producer | budget-service outbox (`OutboxPublisher`) |
| Consumer | wallet-service (`AllowanceDisbursementRequestedConsumer`) |
| Aggregate | `allowance_request` / request id |
| Business idempotency | `allowance-disbursement:{requestId}` (ledger) |
| Event idempotency | wallet `inbox_events.event_id` |

```json
{
  "requestId": "uuid",
  "familyId": "uuid",
  "requesterMemberId": "uuid",
  "amount": 150000,
  "currencyCode": "VND",
  "reason": "string"
}
```

Wallet resolves wallets authoritatively (does **not** trust client wallet IDs):

- Source: single ACTIVE `FAMILY_POOL` for `familyId`
- Destination: single ACTIVE `MEMBER_WALLET` with `ownerMemberId = requesterMemberId` in the same family

Posting: DEBIT source `WALLET_LIABILITY`, CREDIT destination `WALLET_LIABILITY` (`INTERNAL_TRANSFER`).
Reference id: `allowance:{requestId}`.

### AllowanceDisbursementCompleted

| Field | Value |
|-------|--------|
| Topic | `wallet.domain.events` |
| Producer | wallet outbox (same TX as successful ledger post / recoverable on idempotent replay) |
| Consumer | budget-service (`WalletDisbursementOutcomeConsumer`) → `DISBURSED` |

Payload includes `requestId`, `familyId`, `sourceWalletId`, `destinationWalletId`, `transactionId`, `amount`, `currencyCode`, `referenceId`.

### AllowanceDisbursementFailed

Permanent business failures (insufficient funds, missing/ambiguous wallets, ownership/family mismatch, invalid amount/currency). No ledger posting. Budget → `DISBURSEMENT_FAILED`.

Payload: `requestId`, `familyId`, `errorCode`, `message` (safe diagnostics only).

Transient failures (DB/Kafka) leave inbox `FAILED` / outbox retryable and must not be converted into `DISBURSEMENT_FAILED`.

### Local testing

```bash
mvn -f services/budget-service/pom.xml test
mvn -f services/wallet-service/pom.xml test
WALLET_IT=true WALLET_IT_JDBC_URL=jdbc:postgresql://localhost:5432/wallet_db \
  mvn -f services/wallet-service/pom.xml test -Dtest='*IT'
```

## InventoryLow

| Field | Value |
|-------|--------|
| Topic | `household.domain.events` |
| Producer | household-service transactional outbox (`OutboxPublisher`) |
| Consumer | notification-service (`NotificationKafkaConsumers.onHousehold`) |
| Event type | `InventoryLow` |
| Aggregate | `inventory_item` / inventory item id |
| Partition key | `familyId` (fallback aggregateId) |
| Idempotency | `inbox_events.event_id` = outbox row id (`eventId`) |

### Payload (required)

```json
{
  "inventoryItemId": "uuid",
  "familyId": "uuid",
  "quantityOnHand": 2,
  "reorderLevel": 5
}
```

No recipient fields are included. Domain dedup: `inventory_items.low_stock_alerted` prevents a second outbox write until stock rises above reorder level.

### Recipient-resolution policy

1. Notification Service calls Family Service `GET /internal/families/{familyId}/members` (header `X-Internal-Api-Key`).
2. Active members only (`status=ACTIVE`; `LEFT` / `REMOVED` excluded by Family query).
3. Alert roles: **OWNER** and **PARENT** only (CHILD / VIEWER / ADULT_MEMBER excluded).
4. Recipient identity = `userProfileId` (same UUID used as `notifications.recipient_user_id`).
5. One in-app notification per recipient; inbox `eventId` prevents fan-out replay duplicates.

### Retry / failure

- Household outbox: publish after commit; mark `PUBLISHED` only after Kafka ack; on broker failure stay/mark `FAILED` and retry on next poll (pessimistic row lock on claim).
- Notification: Family lookup failure before inbox claim → exception → Kafka redelivery. Missing `familyId` → inbox `SKIPPED`. Processed inbox rows are not re-applied.

### Known limitations

- No Kafka DLT wiring for household topic in this package (same as other domain topics).
- Concurrent outbox publishers rely on DB row locks, not a distributed claim lease.
- End-to-end broker tests are not part of the default unit suite (`KAFKA_ENABLED=false`).

## DLQ

Failed consumers after retries → `{topic}.DLT` (configured per service when Kafka enabled).

## Local toggle

```bash
export KAFKA_ENABLED=true
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Default `KAFKA_ENABLED=false` so unit tests run without a broker.
