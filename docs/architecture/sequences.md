# Sequence Diagrams

Canonical workflow docs supersede older “planned” notes:

- [Payment top-up saga](../workflows/PAYMENT_TOPUP_SAGA.md) — **implemented** (FakePayment + optional MoMo Sandbox)
- [Expense wallet settlement](../workflows/EXPENSE_WALLET_SETTLEMENT_SAGA.md) — **implemented**
- [Allowance disbursement](../workflows/ALLOWANCE_DISBURSEMENT_SAGA.md) — partial (Kafka consumer gap on wallet)
- Portfolio demo: [DEMO_SCENARIO.md](../portfolio/DEMO_SCENARIO.md)

## Payment top-up (FakePayment)

```mermaid
sequenceDiagram
  actor User
  participant GW as api-gateway
  participant Pay as payment-service
  participant K as Kafka
  participant Wal as wallet-service
  User->>GW: POST /api/v1/payments (FAKE_PAYMENT)
  GW->>Pay: create order
  User->>GW: POST /api/v1/payments/fake/ipn
  GW->>Pay: mark provider paid + outbox
  Pay->>K: PaymentProviderPaid
  K->>Wal: credit PAYMENT_TOPUP
  Wal->>K: WalletCreditPosted
  K->>Pay: COMPLETED
```

## Expense settlement

```mermaid
sequenceDiagram
  participant Exp as expense-service
  participant K as Kafka
  participant Wal as wallet-service
  Exp->>K: ExpensePaymentRequested
  K->>Wal: EXPENSE_PAYMENT journal
  alt success
    Wal->>K: WalletDebitPosted
    K->>Exp: PAID
  else permanent failure
    Wal->>K: ExpensePaymentFailed
    K->>Exp: FAILED
  end
```
