# Implementation Progress

**Updated:** 2026-11-18 (checkpoint 3)

**Remote:** `origin/main` tracking — push after this commit

## Completed & verified (mvn test / package)

| Area | Notes |
|------|-------|
| Audit + wallet ledger | earlier commits |
| Family authz | earlier |
| Payment MoMo + IPN + saga outbox | 13+ tests; live sandbox pending credentials |
| Profile provision | OK |
| API Gateway + CI + Keycloak realm + Mailpit | OK |
| Wallet Kafka inbox/outbox (optional) | KAFKA_ENABLED=false default |
| Expense wallet-funded workflow | tests OK |
| Budget + allowance saga states | tests OK |
| Household inventory + InventoryLow outbox | tests OK |
| Frontend API v1 wiring | build OK; mock still default |
| K8s starter manifests | structure only |

## Remaining for Definition of Done

- Notification + Analytics event consumers / read models
- JWT enforcement on all services + cross-family IT
- Testcontainers concurrency wallet tests
- Full Compose app containers + observability dashboards
- Frontend complete non-mock flows (wallet list API, Keycloak login)
- FINAL_IMPLEMENTATION_REPORT.md
- MoMo live sandbox verification (needs owner credentials)

## Honest blockers

- MoMo credentials not in env → live IPN unverified
- Subagent model quota limited some parallel work; continued on composer
