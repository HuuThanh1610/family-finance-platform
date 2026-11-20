# Architecture walkthrough (interview)

Source-grounded tour of **Family Finance Platform**. Prefer this over stale “planned” notes in older pages. Evidence levels: see [EVIDENCE_MATRIX.md](EVIDENCE_MATRIX.md).

## 1. Overall system architecture

Nine Spring Boot services + API Gateway, React web, PostgreSQL ×9, Kafka, Keycloak, optional Prometheus/Grafana/OTel/Tempo.

**Sync:** REST through gateway (and some service-to-service HTTP for authz/credits).  
**Async:** Domain events via transactional outbox → Kafka → inbox → handlers.

## 2. Why microservices and Database-per-Service

**Problem:** Wallet money invariants must not share tables with notifications or shopping lists.  
**Approach:** One DB schema ownership per service; no cross-DB FKs (`AGENTS.md`).  
**Evidence:** `infra/postgres/init/01-create-databases.sql`; Flyway under each `services/*/src/main/resources/db/migration`.  
**Trade-off:** Joins across contexts require APIs/events; eventual consistency for read models.  
**Production scale:** Schema ownership stays; add stronger contracts, CDC, and SLOs.

## 3. Responsibility and boundaries

| Service | Owns | Must not |
|---------|------|----------|
| wallet | Journals, balances, wallet accounts | Call other services’ DBs |
| payment | Orders, provider adapters | Post ledger itself |
| budget | Allowance/budget state | Move money without Wallet |
| expense | Expense lifecycle | Mark PAID without Wallet outcome |
| analytics | Projections | Own balances / mutate wallet_db |
| family | Membership + permission answers | Store money |

## 4. API Gateway and JWT authentication

Gateway (`services/api-gateway`, port 8080) routes `/api/**` to services. When `SECURITY_ENABLED=true`, OAuth2 Resource Server validates JWT. **Each service also validates JWT** — gateway is not the only trust boundary (`infra/keycloak/README.md`).

## 5. Family-level authorization

**Problem:** Realm role ≠ membership in a specific family.  
**Approach:** Wallet/budget/expense call Family `GET /internal/families/{familyId}/members/{userProfileId}/authorization` with `X-Internal-Api-Key` (`InternalFamilyAuthorizationController`, `FamilyAuthorizationClient`).  
**Trade-off:** Sync dependency on Family Service for money APIs.  
**Evidence:** Permission keys such as `wallet.transfer`, `budget.approve`, `expense.create`.

## 6. Wallet and double-entry ledger

`LedgerPostingServiceImpl` posts balanced journals: entries sum DEBIT = CREDIT; updates `wallet_balances` in the same TX; writes outbox event (`PAYMENT_TOPUP` → `WalletCreditPosted`, `INTERNAL_TRANSFER` → `WalletTransferPosted`, `EXPENSE_PAYMENT` → `WalletDebitPosted`).

Doc: [wallet-ledger-accounting.md](../database/wallet-ledger-accounting.md).

## 7. Transaction boundaries and pessimistic locking

Balance mutations lock rows (`SELECT … FOR UPDATE` pattern in ledger posting) so concurrent transfers/expenses serialize on liability accounts. Insufficient funds fail before posting.

## 8. Transactional Outbox and Inbox

**Problem:** Dual-write (DB commit + Kafka publish) loses events or publishes without commit.  
**Approach:** Insert `outbox_events` in the business TX; `OutboxPublisher` polls and publishes; consumers record `inbox_events.event_id` UNIQUE before side effects.  
**Evidence:** Wallet/Payment/Budget/Expense/Household outbox tables + publishers; Analytics/Notification/Wallet inboxes.

## 9. At-least-once Kafka delivery

Consumers may see duplicates. Design target: **effectively-once business effects**, not broker exactly-once. Redelivery + UNIQUE constraints.

## 10. Idempotency: event ID vs business ID

| Layer | Key | Example |
|-------|-----|---------|
| Kafka redelivery | `inbox_events.event_id` | Outbox row UUID |
| Ledger posting | `idempotency_key` / `reference_id` | `pay-credit:{orderId}`, `expense-pay:{expenseId}` |
| Analytics cashflow | `realized_financial_postings.ledger_transaction_id` | One TX → one income/expense |

## 11. Allowance Wallet Saga

Budget `AllowanceRequestService.approve` → outbox `AllowanceDisbursementRequested` (`budget.domain.events`).  
Wallet `WalletApplicationService.disburseAllowance` posts `INTERNAL_TRANSFER` and can emit Completed/Failed on `wallet.domain.events`. Budget `WalletDisbursementOutcomeConsumer` closes state.

**Gap (verified):** no `AllowanceDisbursementRequestedConsumer` `@KafkaListener` in wallet-service — Kafka auto-disbursement path incomplete despite docs/tests around the service method.

## 12. Expense Wallet Settlement Saga

`ExpenseService.submitForPayment` → `ExpensePaymentRequested` → `ExpensePaymentRequestedConsumer` → `EXPENSE_PAYMENT` journal → `WalletDebitPosted` or `ExpensePaymentFailed` → Expense PAID/FAILED. HTTP mark-paid shortcut removed (Package D).

## 13. Inventory Low-Stock notification flow

`InventoryService.adjust` when quantity ≤ reorder → outbox `InventoryLow` (`household.domain.events`) → `NotificationKafkaConsumers.onHousehold` → Family members API → notify OWNER/PARENT only. Dedup: `inventory_items.low_stock_alerted`.

## 14. Analytics authoritative projections

`AnalyticsProjectionService` (Package E): income = `WalletCreditPosted`+`PAYMENT_TOPUP`; expense = `WalletDebitPosted`+`EXPENSE_PAYMENT`; ignore intents/failures/transfers. Inbox FAILED retryable. [analytics-metrics.md](../architecture/analytics-metrics.md).

## 15. Flyway and schema management

Each service owns numbered migrations (`V1__…`, …). Never share another service’s schema. Analytics added `V102__realized_financial_postings.sql` for business idempotency.

## 16. Testing strategy

- Per-module Maven unit tests (CI matrix).  
- Wallet `*IT` against PostgreSQL (GHA service container; assert **14** tests).  
- Scripts: `runtime-e2e-smoke.sh` (FakePayment→credit), `runtime-security-verify.sh`.  
- Default unit runs with `KAFKA_ENABLED=false` so broker not required.

## 17. GitHub Actions and CI

`.github/workflows/ci.yml`: compose-config, frontend lint/build, backend matrix (10 modules), wallet-integration-tests. CI proves compile/tests, not production deploy.

## 18. Observability and tracing

Actuator Prometheus + Micrometer OTLP. Compose profile `observability`. Historical verification showed shared W3C trace across gateway and family-service; not every saga is Tempo-scripted.

## 19. Failure modes and recovery

| Failure | Behavior |
|---------|----------|
| Kafka down after outbox insert | Outbox stays PENDING/FAILED; publisher retries |
| Consumer crash mid-handler | Inbox FAILED / redelivery; wallet/analytics retry policies differ |
| Duplicate Fake IPN | Ledger idempotency → one credit |
| Family Service down | Money APIs needing authz fail closed |
| Insufficient funds | Permanent business failure events; no journal |

## 20. Trade-offs and future improvements

- Microservices overhead vs clear money boundary — justified for portfolio learning; a modular monolith could teach the same ledger with less ops.  
- Close allowance Kafka consumer gap.  
- Stronger DLT / poison-message ops.  
- Analytics rebuild job.  
- K8s manifests remain starters (`infra/k8s/README.md`).
