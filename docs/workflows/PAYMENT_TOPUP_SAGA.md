# Saga: MoMo top-up → wallet credit

```text
User                Payment Service           MoMo            Wallet Service
 |                       |                     |                   |
 |-- create top-up ----->|                     |                   |
 |                       |-- create order      |                   |
 |                       |-- HMAC create ----->|                   |
 |<------ payUrl --------|                     |                   |
 |---- redirect -------->| (browser)           |                   |
 |                       |<------ IPN ---------|                   |
 |                       | verify HMAC/amount  |                   |
 |                       | PROVIDER_PAID       |                   |
 |                       | CREDIT_PENDING      |                   |
 |                       | outbox PaymentProviderPaid              |
 |                       |-------- Kafka ----->|                   |
 |                       |                     | credit ledger     |
 |                       |                     | WalletCreditPosted|
 |                       |<------- Kafka ------|                   |
 |                       | COMPLETED           |                   |
```

## Recovery

- Duplicate IPN / Kafka: webhook_inbox + ledger idempotency key `pay-credit:{paymentOrderId}`
- Wallet down after provider paid: order stays `CREDIT_PENDING`; reconcile/retry outbox
- Do **not** auto-refund MoMo solely because Wallet is temporarily unavailable
