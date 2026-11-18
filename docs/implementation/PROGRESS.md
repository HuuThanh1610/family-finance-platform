# Implementation Progress

**Updated:** 2026-11-18 (checkpoint 4)  
**Remote:** `origin/main` @ `4220c4a`

## Completed & verified

| Area | Evidence |
|------|----------|
| Audit, wallet ledger, family authz | earlier commits + `mvn test` |
| Payment MoMo + IPN + Fake + saga outbox | payment `mvn test` |
| Profile provision | profile `mvn test` |
| API Gateway, CI, Keycloak realm, Mailpit | packaged / docs |
| Expense wallet payment workflow | expense `mvn test` |
| Budget + allowance saga | budget `mvn test` |
| Household inventory + InventoryLow | household `mvn test` |
| Notification inbox + preferences | notification `mvn test` |
| Analytics CQRS projections | analytics `mvn test` |
| Frontend `/api/v1` client wiring | `npm run build` |
| Observability Compose + Micrometer | prometheus profile |
| Wallet concurrent IT (Testcontainers) | gated `WALLET_IT=true` |

## Remaining for full Definition of Done

1. JWT Resource Server enforced on all services + cross-family security IT
2. Run `WALLET_IT=true` against Docker and record pass/fail honestly
3. Live MoMo sandbox (needs owner credentials + public IPN tunnel)
4. Frontend Keycloak login + non-mock wallet list bootstrap
5. Full Compose images for every Spring service
6. Expand Grafana dashboards / OTel collectors
7. Refresh FINAL report with exact test counts after a full matrix run

## Blockers

- MoMo sandbox credentials not provided
- Some subagent model quotas failed mid-run; work continued locally
