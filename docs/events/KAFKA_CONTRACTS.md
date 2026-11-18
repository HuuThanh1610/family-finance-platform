# Kafka Event Contracts

Delivery: **at-least-once**. Consumers must be idempotent via `inbox_events.event_id` UNIQUE.

## Topics

| Topic | Producers | Consumers | Key |
|-------|-----------|-----------|-----|
| `payment.provider.events` | payment-service outbox | wallet-service | `paymentOrderId` / walletId |
| `wallet.ledger.events` | wallet-service outbox | payment, notification, analytics | `walletId` or transaction aggregate |
| `wallet.domain.events` | wallet-service | analytics, notification | aggregateId |
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

## DLQ

Failed consumers after retries → `{topic}.DLT` (configured per service when Kafka enabled).

## Local toggle

```bash
export KAFKA_ENABLED=true
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Default `KAFKA_ENABLED=false` so unit tests run without a broker.
