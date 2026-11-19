# Allowance Disbursement Saga

## Happy path

1. OWNER/PARENT calls approve → Budget authz `budget.approve`.
2. Same TX: status `APPROVED_PENDING_DISBURSEMENT`, approval history, outbox `AllowanceDisbursementRequested`.
3. Budget `OutboxPublisher` publishes to `budget.domain.events` after commit; `PUBLISHED` only after Kafka ack.
4. Wallet consumes event (inbox by `eventId`), resolves FAMILY_POOL + MEMBER_WALLET, posts balanced transfer with idempotency `allowance-disbursement:{requestId}`.
5. Same TX as ledger: outbox `AllowanceDisbursementCompleted` on `wallet.domain.events`.
6. Budget consumes completion → `DISBURSED` (idempotent if already disbursed).

## Failure path

Permanent Wallet errors emit `AllowanceDisbursementFailed`; Budget → `DISBURSEMENT_FAILED` (not `REJECTED`).

## At-least-once / effectively-once money

| Scenario | Outcome |
|----------|---------|
| Duplicate Budget publish / Kafka redelivery (same eventId) | Wallet inbox skips |
| Different eventId, same requestId | Ledger idempotency returns existing TX; completion outbox ensured once |
| Completion redelivered | Budget inbox skips / DISBURSED no-op |
| Insufficient funds | No posting; failure event; Budget `DISBURSEMENT_FAILED` |
| Wrong family / member wallet | Permanent failure, no posting |
| Concurrent allowances | Liability row locks + balance check; loser gets insufficient funds |
| Kafka down on Budget publish | Outbox stays `FAILED`, retried |

Canonical contracts: `docs/events/KAFKA_CONTRACTS.md`.
