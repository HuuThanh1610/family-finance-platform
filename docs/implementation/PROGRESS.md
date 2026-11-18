# Implementation Progress

**Updated:** 2026-11-18 (runtime integration verification)  
**Remote:** `origin/main`  
**HEAD:** see `git log -1 --oneline` after push

## Completed & verified this session

| Area | Evidence |
|------|----------|
| Full compose stack healthy | All `ff-*` app containers healthy (gateway, frontend, 9 services, kafka, keycloak, tempo, prometheus, grafana) |
| JWT Resource Server (integrated) | Anon `/api/families` → **401**; invalid JWT → **401**; issuer `http://localhost:8180/realms/family-finance` |
| Cross-family / role authz (runtime) | `scripts/runtime-security-verify.sh` — non-member/cross-family/CHILD approve/VIEWER transfer → **403** |
| FakePayment → Kafka → wallet credit | `SECURITY_ENABLED=true KAFKA_ENABLED=true ./scripts/runtime-e2e-smoke.sh` — credit **50000 once** after double IPN |
| Kafka broker | Topics + groups `wallet-service`/`payment-service`; outbox `PUBLISHED=1`; inbox `PROCESSED=1`; ledger DEBIT=CREDIT |
| OTLP → Tempo | Collector receives spans; Tempo search populated; forced W3C `traceparent` shows **api-gateway + family-service** |
| Wallet IT | **9/9 PASS** on compose `wallet_db` (`WALLET_IT=true` + JDBC) |
| payment / budget / profile / family unit | payment **24**; budget **7**; profile **5**; family **17** PASS |
| Frontend | `npm run build` PASS (lint warnings only) |
| Compose config | `docker compose --profile full --profile observability config -q` PASS |

## Remaining / blocked

1. **Live MoMo sandbox E2E** — BLOCKED (owner credentials + public HTTPS IPN)
2. **Testcontainers auto-start** — BLOCKED on Docker Desktop 29 (HTTP 400 to docker-java); use `WALLET_IT_JDBC_URL`
3. **Full allowance/expense/inventory Kafka saga demos** — payment→wallet credit proven; other sagas code-present, not all scripted in this runtime pass
4. **K8s live deploy** — manifests only

## Quality gate snapshot (2026-11-18 runtime)

| Suite | Result |
|-------|--------|
| family-service | 17 run / 0 fail |
| payment-service | 24 run / 0 fail |
| budget-service | 7 run / 0 fail |
| profile-service | 5 run / 0 fail |
| wallet unit (`!*IT`) | 7 run / 0 fail |
| wallet IT | 9 run / 0 fail / 0 skip |
| frontend `npm run build` | PASS |
| `docker compose config -q` | PASS |
| runtime smoke (FakePayment+Kafka) | PASS |
| runtime security verify | PASS |
