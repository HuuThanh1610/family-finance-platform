# Expense Wallet Settlement Saga

## Happy path

1. Authorized member (`expense.create`) submits payment with a wallet id.
2. Same TX: status `PENDING_WALLET`, `paymentReference=expense:{id}`, outbox `ExpensePaymentRequested`.
3. Expense `OutboxPublisher` → `expense.domain.events` (key=`familyId`); `PUBLISHED` only after Kafka ack.
4. Wallet inbox by `eventId` → `payExpense`: validate wallet, DEBIT liability / CREDIT `EXPENSE_CLEARING`.
5. Ledger outbox emits `WalletDebitPosted` on `wallet.ledger.events`.
6. Expense consumes debit → `PAID` when family/wallet/amount/reference match.

## Failure path

Permanent Wallet errors → `ExpensePaymentFailed` on `wallet.domain.events` → Expense `FAILED`.
Resubmit from `FAILED` → `SUBMITTED_FOR_PAYMENT` path is allowed by the state machine.

## At-least-once / effectively-once money

| Scenario | Outcome |
|----------|---------|
| Duplicate publish / same eventId | Wallet inbox skip |
| Different eventId, same expenseId | Ledger idempotency `expense-pay:{id}` |
| WalletDebitPosted replay | Expense inbox skip / PAID no-op |
| Mismatched family/wallet/amount | Expense SKIPPED; not PAID |
| Insufficient funds | No journal; failure event |
| Concurrent expenses on one wallet | Liability locks; loser insufficient funds |
| Kafka down on publish | Outbox `FAILED`, retried |

Canonical contracts: `docs/events/KAFKA_CONTRACTS.md`.
Accounting: `docs/database/wallet-ledger-accounting.md`.
