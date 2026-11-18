# INITIAL AUDIT — Family Finance Platform

**Date:** 2026-11-18  
**Branch:** `main` @ `6f2779f` (synced with `origin/main`)  
**Remote:** `https://github.com/HuuThanh1610/family-finance-platform.git` (PRIVATE)

## 1. Repository snapshot

| Area | Status |
|------|--------|
| Git history | 14 Conventional Commits preserved; working tree clean |
| Frontend | Professional minimalist UI; **default mock** (`VITE_USE_MOCK=true`) |
| Backend | 9 Spring Boot 3.3.5 scaffolds; **read-only GET**; business logic TODO |
| API Gateway | **Missing** |
| Flyway SQL | Present per service (~42–84 migration files); schemas sound |
| Docker Compose | Postgres running healthy; Kafka/Keycloak/Redis behind `full` profile only |
| Keycloak realm | Placeholder README only — **no realm import** |
| Tests | Scaffold README only — **no real unit/integration tests** |
| CI/CD | **Missing** |
| Observability | Actuator health only |
| Kubernetes | **Missing** |

Verified commands:
- `mvn -f services/wallet-service/pom.xml -DskipTests package` → OK
- `docker compose ps` → `ff-postgres` healthy
- Frontend previously built successfully; still mock-first

## 2. Implementation matrix

| Service | Port | Build | Endpoints | Business logic | Security | Kafka/Outbox | Tests | Gaps |
|---------|------|-------|-----------|----------------|----------|--------------|-------|------|
| profile-service | 8081 | Yes | GET profiles | None | Placeholder | No | No | CRUD, Keycloak sub provisioning |
| family-service | 8082 | Yes | GET families | None | Placeholder | No | No | Invite, roles, resource auth |
| wallet-service | 8083 | Yes | GET wallets | `LedgerPostingService` interface only | Placeholder | Schema only | No | **Full ledger, transfer, locks** |
| payment-service | 8084 | Yes | GET + POST 501 | `PaymentProvider` interface only | Placeholder | Schema only | No | MoMo, IPN, saga credit |
| budget-service | 8085 | Yes | GET budgets | None | Placeholder | Schema only | No | Allowance saga |
| expense-service | 8086 | Yes | GET expenses | None | Placeholder | No | No | Wallet-funded workflow |
| household-service | 8087 | Yes | GET shopping | None | Placeholder | No | No | Inventory mutations, alerts |
| notification-service | 8088 | Yes | GET notifications | None | Placeholder | Schema only | No | Event consumers |
| analytics-service | 8089 | Yes | GET summaries | None | Placeholder | No | No | Event projections |
| api-gateway | — | **No** | — | — | — | — | — | Must create |

## 3. Database findings

**Strengths**
- DB-per-service with init script creating users/DBs
- Wallet ledger: positive amounts, DEBIT/CREDIT, immutable entries, `version` on balances, outbox/inbox tables
- Seed UUID hex issue was fixed earlier; debit=credit verified on wallet seed

**Issues / forward work**
- `wallet_type` CHECK uses `MEMBER_POCKET` / `ALLOWANCE_JAR` / `SAVINGS`; product API prefers `MEMBER_WALLET` → add **forward** migration (do not edit V1)
- V99 seeds mixed into migration path — need Flyway profiles or separate seed runner for prod safety
- Manual `psql` apply vs Flyway history may diverge on local volume; services using Flyway on recreate is preferred
- No cross-DB FKs (correct); authorization must be enforced via APIs

## 4. Frontend findings

- Routes & screens complete against mock adapters
- Design system: monochrome minimalist (post `6f2779f`)
- `services/family-finance-api.ts` short-circuits when `USE_MOCK`
- PaymentProvider abstraction exists on FE; backend stubs 501
- No Keycloak/OIDC login flow

## 5. Architecture reconciliation decisions

1. Keep monorepo + independently buildable Maven modules.
2. Add `api-gateway` as new Spring Cloud Gateway app.
3. Implement real ledger/payment/saga/security in place (replace TODOs).
4. New Flyway versions only — never rewrite applied migrations.
5. Dev auth: JWT Resource Server + Keycloak realm import; test profile may use test JWT keys.
6. Kafka required for payment/allowance/expense workflows; enable by default in compose for app services.
7. Frontend switches to real APIs behind env flag once gateway is up; keep FakePaymentAdapter for CI.

## 6. Priority backlog (execution order)

1. Wallet double-entry + transfers + concurrency + tests
2. Family membership + resource authorization helpers
3. Payment MoMo adapter + IPN + outbox + wallet credit consumer
4. Budget/allowance saga; expense debit workflow
5. Household, notification consumers, analytics projections
6. API Gateway + Keycloak + compose full stack
7. Frontend real API wiring
8. CI, observability, K8s manifests, docs, push

## 7. Risks / blockers

| Risk | Mitigation |
|------|------------|
| No MoMo sandbox credentials | Full adapter + WireMock/FakePaymentAdapter; document pending live verify |
| Scope size vs context limits | Milestone commits + `PROGRESS.md` checkpoints |
| Local DB already migrated outside Flyway | Prefer recreate volume or `baseline` carefully |

---

**Conclusion:** Repository is a solid scaffold with good DB design and polished mock UI. **Critical financial, payment, security, and distributed workflows are not implemented.** This audit supersedes earlier “intentionally left unimplemented” reports — implementation proceeds immediately from Phase B/D.
