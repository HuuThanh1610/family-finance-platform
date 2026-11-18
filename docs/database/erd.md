# Database ERD

## wallet_db (core accounting)

```mermaid
erDiagram
  wallet_accounts ||--o{ ledger_accounts : has
  wallet_accounts ||--|| wallet_balances : has
  wallet_accounts ||--o{ ledger_transactions : posts
  ledger_transactions ||--|{ ledger_entries : contains
  ledger_accounts ||--o{ ledger_entries : booked_to
  wallet_accounts {
    uuid id PK
    uuid family_id
    varchar type
    varchar currency
  }
  ledger_transactions {
    uuid id PK
    varchar reference_id UK
    varchar idempotency_key UK
  }
  ledger_entries {
    uuid id PK
    varchar entry_type
    bigint amount
  }
  wallet_balances {
    uuid wallet_account_id PK
    bigint available_balance
    bigint version
  }
```

**Invariant:** với mỗi `ledger_transactions`, Σ DEBIT amount = Σ CREDIT amount (enforce ở application layer).

## family_db

```mermaid
erDiagram
  families ||--o{ family_members : has
  families ||--o{ family_invitations : invites
  family_roles ||--o{ family_permissions : grants
  families ||--o{ family_audit_logs : audits
  families {
    uuid id PK
    varchar name
  }
  family_members {
    uuid id PK
    uuid user_id
    varchar role
  }
```

## payment_db

```mermaid
erDiagram
  payment_orders ||--o{ payment_attempts : tries
  payment_orders ||--o{ provider_transactions : maps
  payment_orders ||--o{ payment_refunds : refunds
  payment_orders ||--o{ payment_reconciliations : reconciles
  webhook_inbox {
    uuid id PK
    varchar provider
    varchar payload_hash
  }
```

## budget_db

```mermaid
erDiagram
  budgets ||--o{ budget_categories : splits
  allowance_policies ||--o{ allowance_requests : generates
  allowance_requests ||--o{ approval_history : reviewed
```

Xem thêm bảng đầy đủ tại [schema-overview.md](./schema-overview.md).
