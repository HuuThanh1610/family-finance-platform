# Sequence Diagrams

## Payment top-up (planned)

```mermaid
sequenceDiagram
  actor User
  participant Web
  participant Payment as payment-service
  participant Provider as MoMo Sandbox
  participant Wallet as wallet-service
  User->>Web: Chọn số tiền / provider
  Web->>Payment: POST /api/payments (idempotency-key)
  Payment->>Payment: Tạo payment_orders PENDING
  Payment->>Provider: Create payment (signed)
  Provider-->>Payment: payUrl / orderId
  Payment-->>Web: checkout info
  Provider-->>Payment: Webhook callback
  Note over Payment: Verify signature + dedupe webhook_inbox
  Payment->>Wallet: Credit wallet (via event/API)
  Note over Wallet: Double-entry post + balance update
  Payment-->>Web: STATUS SUCCESS
```

**Chưa triển khai:** signing, webhook verify, wallet credit, duplicate callback protection.

## Wallet transfer (planned)

```mermaid
sequenceDiagram
  actor User
  participant Web
  participant Wallet as wallet-service
  User->>Web: Transfer A → B
  Web->>Wallet: POST /transfers (idempotency-key)
  Wallet->>Wallet: Begin TX
  Wallet->>Wallet: Lock/version balances
  Wallet->>Wallet: Insert ledger_transactions + entries
  Wallet->>Wallet: Verify Σ debit = Σ credit
  Wallet->>Wallet: Update wallet_balances
  Wallet->>Wallet: Write outbox_events
  Wallet-->>Web: Transfer result
```

**Chưa triển khai:** posting service, locking strategy, consistency verifier.
