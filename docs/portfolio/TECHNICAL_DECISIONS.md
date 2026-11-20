# Technical decisions

Rationale below is the **current design rationale** grounded in the repository. Original decision chronology is not reconstructed where unknown.

---

### 1. Microservices + database-per-service

**Context:** Multiple bounded contexts (identity, family, money, payments, ops).  
**Decision:** Separate deployable Spring Boot services, each with its own PostgreSQL database and Flyway history.  
**Alternatives:** Modular monolith; shared DB with schemas.  
**Trade-offs:** Operational complexity vs isolation of wallet data.  
**Status/evidence:** `services/*`, `01-create-databases.sql`, Compose `full`.

---

### 2. Wallet ledger as sole financial authority

**Context:** Payment/expense/budget must not invent balances.  
**Decision:** Only `LedgerPostingServiceImpl` posts journals and updates `wallet_balances`.  
**Alternatives:** Payment service updates balances; eventual balance sync.  
**Trade-offs:** All money flows depend on Wallet; clearer audit trail.  
**Status/evidence:** Ledger docs + Package D/E consumers; Analytics never writes wallet_db.

---

### 3. Transactional Outbox / Inbox

**Context:** Avoid dual-write between DB and Kafka.  
**Decision:** Outbox row in business TX; poller publishes; inbox UNIQUE `event_id`.  
**Alternatives:** Listen to DB WAL/CDC; sync Kafka TX (fragile).  
**Trade-offs:** Polling lag; need publisher ops.  
**Status/evidence:** Outbox publishers across payment/wallet/budget/expense/household.

---

### 4. Idempotency by event ID and business key

**Context:** At-least-once delivery duplicates events.  
**Decision:** Inbox event UUID + ledger `idempotency_key`/`reference_id` + Analytics `ledger_transaction_id`.  
**Alternatives:** Broker exactly-once only.  
**Trade-offs:** App complexity; correct under redelivery.  
**Status/evidence:** Migrations UNIQUE constraints; Package E `realized_financial_postings`.

---

### 5. Kafka at-least-once semantics

**Context:** Simple Compose Kafka, Spring listeners.  
**Decision:** Design for duplicates; effectively-once effects.  
**Alternatives:** Exactly-once transactional produce/consume everywhere.  
**Trade-offs:** Simpler ops; requires careful consumer design.  
**Status/evidence:** Contracts doc header; inbox patterns.

---

### 6. Internal transfer vs external expense

**Context:** Allowance moves money inside the household; groceries leave the household.  
**Decision:** `INTERNAL_TRANSFER` between liabilities; `EXPENSE_PAYMENT` to `EXPENSE_CLEARING`. Analytics ignores transfers for realized spend.  
**Alternatives:** Treat allowance as expense.  
**Trade-offs:** Two models to teach; prevents double-counting household cashflow.  
**Status/evidence:** Ledger accounting doc; Analytics allowlist.

---

### 7. Analytics authoritative event selection

**Context:** Counting both `PaymentProviderPaid` and `WalletCreditPosted` double-counts.  
**Decision:** Realized cashflow only from Wallet posting events with typed transaction kinds (Package E).  
**Alternatives:** Count provider paid as income; CQRS from payment DB.  
**Trade-offs:** Analytics lag until wallet posts; category fields may be sparse on ledger payloads.  
**Status/evidence:** `AnalyticsProjectionService`, `analytics-metrics.md`.

---

### 8. Keycloak JWT + family-scoped authorization

**Context:** Login identity ≠ “may spend this family’s money”.  
**Decision:** Keycloak for authentication; Family Service for membership permissions; internal API key for S2S.  
**Alternatives:** Embed family claims only in JWT.  
**Trade-offs:** Extra hop; fresher membership truth.  
**Status/evidence:** Realm import, Resource Server YAML, internal authz controllers.

---

### 9. PostgreSQL integration tests in CI

**Context:** Ledger locking/idempotency need a real DB.  
**Decision:** GHA Postgres service + Surefire `*IT` with asserted count **14**.  
**Alternatives:** Testcontainers-only; H2 (insufficient for FOR UPDATE semantics).  
**Trade-offs:** CI time; stronger confidence.  
**Status/evidence:** `.github/workflows/ci.yml` `wallet-integration-tests`.

---

### 10. Docker Compose for local orchestration

**Context:** Reviewers need one-command stack.  
**Decision:** Profiles `full`, `observability`, `web`.  
**Alternatives:** K8s-first local.  
**Trade-offs:** Compose ≠ production topology.  
**Status/evidence:** `docker-compose.yml`, `infra/compose/apps.yml`.

---

### 11. Observability stack

**Context:** Demonstrate metrics/traces for interviews.  
**Decision:** Micrometer + Prometheus/Grafana + OTLP → Tempo.  
**Alternatives:** Vendor APM only.  
**Trade-offs:** Optional profile; not full SLI/SLO product.  
**Status/evidence:** `infra/observability/*`; FINAL report historical Tempo demo.

---

### 12. Explicit non-goals

**Context:** Scope control for a portfolio.  
**Decision:** Not a regulated institution; K8s manifests are starters; no fake uptime/benchmark claims; MoMo Sandbox ≠ production.  
**Alternatives:** Over-claim “enterprise production ready”.  
**Trade-offs:** Honesty over marketing.  
**Status/evidence:** README limitations; `infra/k8s/README.md`; payment docs.
