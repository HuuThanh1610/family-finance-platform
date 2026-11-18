# Implementation Progress

**Updated:** 2026-11-18 (completion hardening)  
**Remote:** `origin/main` (local HEAD ahead until push)  
**HEAD:** `05a5318`

## Completed & verified this session

| Area | Evidence |
|------|----------|
| JWT Resource Server on all Spring services | `0890e16` — `SecurityConfig`, issuer/audience YAML, role mapping |
| Cross-family / role authz unit tests | family-service 17 tests PASS |
| Fail-closed family auth client (wallet) | `FamilyAuthorizationClientTest` PASS |
| Wallet concurrent + ledger IT (PostgreSQL) | `WALLET_IT=true` + `WALLET_IT_JDBC_URL` → **9/9 PASS** (`9bc2569`) |
| Payment recovery / IPN / outbox idempotency | payment **22/22 PASS** (`b23ec92`) |
| Frontend Keycloak OIDC PKCE + real APIs | `npm run build` PASS; mock only if `VITE_USE_MOCK=true` (`b1c44f8`) |
| Dockerfiles + Compose full profile | `docker compose config -q` PASS (`05a5318`) |
| Grafana/Prometheus/OTel | real Spring metric names; collector health checked |

## Remaining / blocked

1. **Live MoMo sandbox E2E** — BLOCKED (owner credentials + public HTTPS IPN)
2. **Testcontainers auto-start** — BLOCKED on this machine: Docker Desktop 29 returns HTTP 400 to docker-java; IT verified via compose Postgres (`wallet_it_db`)
3. **Kafka broker E2E IT** — code present; full broker suite not recorded as PASS this session
4. **`docker compose --profile full up --build`** — config validated; full image build/start not timed as PASS
5. **K8s live deploy** — manifests improved; no kind deploy PASS claimed

## Quality gate snapshot (2026-11-18)

| Suite | Result |
|-------|--------|
| profile / family / budget / expense / household / notification / analytics | unit PASS |
| payment-service | 22 run / 0 fail |
| wallet unit (`!*IT`) | PASS |
| wallet IT (`WALLET_IT=true` + external JDBC) | 9 run / 0 fail / 0 skip |
| frontend `npm run build` | PASS |
| `docker compose config -q` | PASS |
