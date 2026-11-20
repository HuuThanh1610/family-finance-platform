# REST API Overview

**Gateway base:** `http://localhost:8080` (Compose profile `full`).

Default frontend env (`.env.example`): `VITE_API_BASE_URL=http://localhost:8080`, `VITE_USE_MOCK=false`.

Canonical demo paths and DTOs: [docs/portfolio/DEMO_SCENARIO.md](../portfolio/DEMO_SCENARIO.md).  
Per-service OpenAPI: `http://localhost:<port>/swagger-ui.html`.

## Service ports and primary public prefixes

| Service | Port | Primary prefixes (via gateway) |
|---------|------|--------------------------------|
| api-gateway | 8080 | `/api/**` |
| profile-service | 8081 | `/api/v1/profiles` |
| family-service | 8082 | `/api/families` |
| wallet-service | 8083 | `/api/v1/wallets`, `/api/v1/ledger`, `/api/v1/wallet-transfers` |
| payment-service | 8084 | `/api/v1/payments`, `/api/v1/payments/fake`, `/api/v1/payments/momo` |
| budget-service | 8085 | `/api/v1/budgets`, `/api/v1/allowances` |
| expense-service | 8086 | `/api/v1/expenses` |
| household-service | 8087 | `/api/v1/household` |
| notification-service | 8088 | `/api/v1/notifications` |
| analytics-service | 8089 | `/api/v1/analytics` |

Internal routes (`/internal/**`) are not exposed for browser demos; they use `X-Internal-Api-Key`.

## Domain events

Prefer actual outbox event type names in [docs/events/KAFKA_CONTRACTS.md](../events/KAFKA_CONTRACTS.md) (e.g. `PaymentProviderPaid`, `WalletCreditPosted`, `ExpensePaymentRequested`, `InventoryLow`) over legacy dotted placeholders.
