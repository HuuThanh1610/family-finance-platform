# Evidence matrix (Package F audit)

Levels of evidence (distinct):

| Level | Meaning |
|-------|---------|
| **Code** | Present in source |
| **Unit** | Covered by module unit tests |
| **IT** | Covered by `*IT` / Postgres IT |
| **Compose** | Documented for / verified against Docker Compose `full` |
| **CI** | Asserted or executed in GitHub Actions |
| **Live** | Deployed to a non-local production/staging cluster |

Nothing below claims **Live** deployment.

| Capability | Evidence source | Verified status | Limitation |
|------------|-----------------|-----------------|------------|
| Java 21 / Spring Boot 3.3.5 | Service `pom.xml` parents | Code + CI | No multi-module BOM aggregator |
| React 19 / Vite / TypeScript | `frontend/web/package.json` | Code + CI frontend job | UI is portfolio shell, not full product UX |
| 9 services + API Gateway | `infra/compose/apps.yml`, service ports | Code + Compose | Not all APIs equally mature |
| Database-per-service | Flyway per service, `01-create-databases.sql` | Code + Compose | No cross-DB FK by design |
| Double-entry ledger | `LedgerPostingServiceImpl`, `wallet-ledger-accounting.md` | Code + Unit + IT (wallet) | VND BIGINT only |
| Payment top-up saga (FakePayment) | `PaymentOrderService`, consumers, `runtime-e2e-smoke.sh` | Code + Unit + Compose script | Live MoMo IPN needs owner credentials |
| MoMo Sandbox adapter | `Momo*` classes, `docs/payment/MOMO_SANDBOX.md` | Code | Not equivalent to production payments |
| Expense wallet settlement | `ExpensePaymentRequestedConsumer`, Package D | Code + Unit + Wallet IT | Category enrichment in Analytics limited |
| Allowance disbursement logic | `WalletApplicationService.disburseAllowance`, Budget outbox + outcome consumer | Code + Unit | **No** `AllowanceDisbursementRequestedConsumer` Kafka listener in wallet-service — E2E Kafka path incomplete |
| InventoryLow → Notification | Household outbox, `NotificationKafkaConsumers` | Code + Unit | Needs seed inventory; recipient policy OWNER/PARENT |
| Analytics authoritative cashflow | Package E `AnalyticsProjectionService` | Code + Unit | Historical doubles need ops rebuild |
| Keycloak JWT | Realm JSON, Resource Server configs | Code + Compose | Dev passwords in realm; not production IdP hardening |
| Family-scoped authz | `InternalFamilyAuthorizationController`, clients | Code + scripts | Depends on `FAMILY_AUTH_ENABLED` |
| Internal API key | `X-Internal-Api-Key` filters | Code | Shared secret in `.env` for local |
| Kafka Outbox/Inbox | Per-service outbox/inbox tables + publishers | Code | At-least-once; DLT not fully wired for all topics |
| CI Maven matrix | `.github/workflows/ci.yml` | CI | Unit tests; Kafka broker not required |
| Wallet IT = 14 on GHA Postgres | `wallet-integration-tests` job | CI | Testcontainers path optional locally |
| Prometheus / Grafana / OTel / Tempo | `infra/observability/*`, compose profile | Compose | Trace demo verified historically gateway↔family; not every saga |
| Kubernetes manifests | `infra/k8s/` | Code structure only | **Not** a verified live cluster deploy |

## Audit findings affecting demos

1. Prefer `scripts/runtime-e2e-smoke.sh` for FakePayment → wallet credit (most reliable E2E).
2. Expense settlement is implemented end-to-end in code; demonstrate via APIs when Kafka enabled.
3. Allowance: show Budget approve → outbox event; do **not** claim automatic Wallet transfer via Kafka until a wallet consumer is added.
4. Prefer source/config/tests over stale “planned” wording in older architecture pages.
