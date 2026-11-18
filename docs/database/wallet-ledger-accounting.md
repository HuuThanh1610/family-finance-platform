# Wallet Ledger Accounting Guide

## Model

Internal wallets are **liability** accounts (money the platform "owes" the family/member).

| Concept | Rule |
|---------|------|
| Wallet liability account | `account_code = WALLET_LIABILITY`, `account_kind = LIABILITY`, `normal_balance = CREDIT` |
| Available balance | `credits − debits` (= `wallet_balances.balance_amount`) |
| Entry amounts | Always positive `BIGINT` VND |
| Direction | Explicit `DEBIT` / `CREDIT` |
| Invariant | Per journal transaction: Σ debit = Σ credit |

## Postings

### Internal transfer (A → B, amount X)

1. DEBIT source `WALLET_LIABILITY` X  
2. CREDIT destination `WALLET_LIABILITY` X  

### Sandbox top-up (credit wallet)

1. DEBIT family `PSP_CLEARING` (ASSET) X  
2. CREDIT wallet `WALLET_LIABILITY` X  

### External spend / expense

1. DEBIT wallet `WALLET_LIABILITY` X  
2. CREDIT `EXPENSE_CLEARING` X  

### Reversal

Create a new journal that swaps DEBIT/CREDIT of the original entries. Never mutate posted rows.

## Concurrency

- Lock `wallet_balances` rows with `SELECT … FOR UPDATE` in wallet-id order.
- Update balance projection in the same DB transaction as journal insert.
- Reject if resulting liability balance would go negative.
- Enforce unique `idempotency_key` and `reference_id`.

## Legacy seed note

Early V99 seed used ASSET cash + EQUITY opening. V5 adds `WALLET_LIABILITY` and backfills balances from legacy ASSET rows for continuity.
