# REST API Overview

Gateway/API base (planned): `http://localhost:8080`

Hiện tại Web dùng `VITE_USE_MOCK=true`. Khi tắt mock, client gọi REST theo bảng dưới.

| Service | Port | Prefix | Read endpoints (scaffolding) |
|---------|------|--------|------------------------------|
| profile-service | 8081 | `/api/profiles` | GET `/me` |
| family-service | 8082 | `/api/families` | GET `/current`, GET `/{id}/members` |
| wallet-service | 8083 | `/api/wallets` | GET `/`, GET `/{id}`, GET `/{id}/transactions` |
| payment-service | 8084 | `/api/payments` | GET `/`, POST `/` (skeleton) |
| budget-service | 8085 | `/api/budgets` | GET `/current` |
| expense-service | 8086 | `/api/expenses` | GET `/` |
| household-service | 8087 | `/api/household` | GET `/inventory`, GET `/shopping-list` |
| notification-service | 8088 | `/api/notifications` | GET `/` |
| analytics-service | 8089 | `/api/analytics` | GET `/reports` |

OpenAPI UI (per service): `http://localhost:<port>/swagger-ui.html`

## Domain event naming

```text
<context>.<aggregate>.<event>
```

Examples:

- `wallet.ledger.posted`
- `payment.order.succeeded`
- `budget.allowance.approved`
- `household.inventory.low_stock`
- `family.member.invited`

CloudEvents-compatible payload recommended; schema stubs live in `contracts/events/`.
