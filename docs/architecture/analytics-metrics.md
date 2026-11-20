# Analytics financial metrics

Analytics Service maintains **CQRS read models** in `analytics_db`. It never writes Wallet balances or other services’ databases.

## Realized income

Household income credited from an external top-up that has been **posted** to the Wallet ledger.

| Rule | Value |
|------|--------|
| Authoritative event | `WalletCreditPosted` on `wallet.ledger.events` |
| Required `transactionType` | `PAYMENT_TOPUP` |
| Source | `LedgerPostingServiceImpl` outbox payload |
| Counted once per | `payload.transactionId` (`realized_financial_postings.ledger_transaction_id`) |

**Not counted:** `PaymentProviderPaid` (provider confirmation; credit may still fail/delay).

## Realized expense

Household external spend that has been **posted** as an expense journal.

| Rule | Value |
|------|--------|
| Authoritative event | `WalletDebitPosted` on `wallet.ledger.events` |
| Required `transactionType` | `EXPENSE_PAYMENT` |
| Counted once per | `payload.transactionId` |

**Not counted:** `ExpensePaymentRequested`, `ExpensePaymentFailed`.

Category/member breakdowns use optional payload fields (`categoryCode`, `paidByMemberId`). Ledger payloads today often omit category → bucket `OTHER` until enriched.

## Internal transfer

Movement between wallets in the **same** household (e.g. pool → member allowance).

| Rule | Value |
|------|--------|
| Events | `WalletTransferPosted` (`INTERNAL_TRANSFER`), allowance request/completion |
| Realized income/expense | **No** — assets stay inside the family |

## Allowance disbursement

Budget-approved pocket money via Wallet `INTERNAL_TRANSFER`.

| Event | Analytics cashflow |
|-------|--------------------|
| `AllowanceDisbursementRequested` | Ignored |
| `AllowanceDisbursementCompleted` | Ignored (not household spending) |
| `AllowanceDisbursementFailed` | Ignored |
| `WalletTransferPosted` | Ignored |

Separate allowance-utilization metrics are out of scope for Package E.

## Pending / failed operations

Request and failure events do **not** change realized totals. Only successful ledger postings do.

## Event idempotency

1. **Event ID:** `inbox_events.event_id` UNIQUE. `PROCESSED` / `SKIPPED` → skip. `FAILED` → retry on redelivery.
2. **Business:** `realized_financial_postings.ledger_transaction_id` UNIQUE — one ledger TX → one income or expense effect even if Kafka event IDs differ.
3. Inbox status + projection + realized claim share one Spring `@Transactional` boundary (effectively-once effects under at-least-once delivery).

Malformed events (missing `familyId` / `transactionId`, `amount <= 0`, invalid currency) are logged and do not mutate aggregates; inbox still ends `PROCESSED` so poison payloads do not infinite-retry.

## Historical rebuild / reconciliation

Pre–Package E projections may have **double-counted** top-ups (`PaymentProviderPaid` + `WalletCreditPosted`), expenses (`ExpensePaymentRequested` + `WalletDebitPosted`), and counted allowances as expense.

**Do not** silently truncate production tables from application code.

Safe rebuild (ops, explicit):

1. Snapshot / backup `analytics_db`.
2. Truncate read models: `family_monthly_summaries`, `category_spending_summaries`, `member_spending_summaries`, `budget_category_actuals`, `realized_financial_postings`, and optionally reset `inbox_events` for the replay window.
3. Replay authoritative Kafka topics (`wallet.ledger.events` at minimum) into Analytics, or re-seed from a trusted ledger export.
4. Validate totals against Wallet journal sums for `PAYMENT_TOPUP` / `EXPENSE_PAYMENT`.

Until an ops rebuild runs, historical rows may remain inflated; new events follow the rules above.
