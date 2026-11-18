# Implementation Progress

**Updated:** 2026-11-18

## Completed

| Milestone | Status | Notes |
|-----------|--------|-------|
| INITIAL_AUDIT.md | Done | Commit `4fc2781` |
| Wallet V5 migration | Done | Multi-wallet journals, MEMBER_WALLET, WALLET_LIABILITY |
| Double-entry posting | Done | `LedgerPostingServiceImpl` with balance lock + outbox |
| Transfer / top-up credit / reversal APIs | Done | `/api/v1/wallets*`, `/api/v1/wallet-transfers`, ledger reverse |
| Wallet unit tests | Done | Unbalanced, insufficient, happy path (Mockito) |
| Family service business logic | In progress | Background agent |

## Next

1. Commit wallet ledger milestone
2. Payment MoMo adapter + IPN + FakePaymentAdapter
3. Finish family auth + Keycloak placeholders wired
4. Kafka outbox publisher
5. Budget/expense/household/notification/analytics
6. API Gateway + compose apps
7. Frontend real API
8. Testcontainers concurrency tests
9. CI / docs / push

## Verified commands

- `mvn -f services/wallet-service/pom.xml test` → BUILD SUCCESS
- Postgres `wallet_db` V5 applied on local volume
