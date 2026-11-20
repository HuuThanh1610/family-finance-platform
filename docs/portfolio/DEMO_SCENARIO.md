# Demo scenario (~8–12 minutes)

Repeatable interviewer demo using **API Gateway** `http://localhost:8080`.  
Bodies match current DTOs. Placeholders: `<TOKEN>`, `<familyId>`, `<memberId>`, `<walletId>`, `<orderId>`, `<expenseId>`.

**Safety:** Do not `docker compose down -v` or truncate DBs during a live demo. Seed/demo data is **persistent**. Prefer creating a new family slug rather than wiping volumes.

## Prerequisites

```bash
cp .env.example .env
# Compose full profile sets SECURITY_ENABLED/KAFKA_ENABLED true by default in apps.yml
docker compose --profile full up -d --build
# Optional: --profile observability
```

Tools: `curl`, `jq`. Demo Keycloak user (local only): `owner.demo` / see [infra/keycloak/README.md](../../infra/keycloak/README.md).

**Verified happy path:** FakePayment top-up via `./scripts/runtime-e2e-smoke.sh`.  
**Partial path:** Allowance — Budget outbox only (wallet Kafka consumer missing).  
**Code-complete path:** Expense settlement + inventory alert when Kafka + seeds align.

---

### Step 0 — Health

| | |
|--|--|
| **Purpose** | Confirm gateway/stack |
| **Command** | `curl -s http://localhost:8080/actuator/health \| jq` |
| **Expected** | `"status":"UP"` |
| **Troubleshoot** | `docker compose --profile full ps`; check service logs |

---

### Step 1 — Authenticate (OWNER)

| | |
|--|--|
| **Purpose** | Obtain JWT |
| **Role** | Keycloak user mapped to family OWNER after provision |
| **Command** | Password grant against client `family-finance-services` (web client is PKCE-only): |

```bash
TOKEN=$(curl -fsS -X POST "http://localhost:8180/realms/family-finance/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=family-finance-services" \
  -d "client_secret=${KEYCLOAK_CLIENT_SECRET:-change-me-services-client-secret}" \
  -d "username=owner.demo" \
  -d "password=ChangeMeOwner1!" | jq -r .access_token)
AUTH=(-H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json")
```

| **Expected** | Non-empty `access_token` |
| **Inspect** | Decode JWT `sub` |
| **Troubleshoot** | Keycloak not ready; wrong client secret in `.env` / realm |

---

### Step 2 — Provision profile

| | |
|--|--|
| **Purpose** | Map Keycloak `sub` → profile UUID |
| **Endpoint** | `POST /api/v1/profiles/provision` |
| **Body** (`ProvisionProfileRequest`) | |

```json
{
  "keycloakSub": "<jwt-sub>",
  "email": "owner.demo@example.local",
  "displayName": "Owner Demo"
}
```

| **Expected** | 200 with profile `id` |
| **Follow-up** | `GET /api/v1/profiles/me` |

---

### Step 3 — Create family

| | |
|--|--|
| **Purpose** | Shared financial space |
| **Endpoint** | `POST /api/families` |
| **Role** | Authenticated user becomes OWNER member |
| **Body** (`CreateFamilyRequest`) | |

```json
{
  "name": "Demo Family",
  "slug": "demo-family-<timestamp>",
  "description": "Portfolio demo"
}
```

| **Expected** | 201/200 with `id` → `<familyId>` |
| **Inspect** | `GET /api/families/<familyId>/members` → `<memberId>` |
| **Note** | Creates persistent family row |

---

### Step 4 — Create wallets

| | |
|--|--|
| **Purpose** | Pool (+ optional member wallet) |
| **Endpoint** | `POST /api/v1/wallets` |
| **Body** (`CreateWalletRequest`) | |

```json
{
  "familyId": "<familyId>",
  "ownerMemberId": "<memberId>",
  "name": "Family Pool",
  "walletType": "FAMILY_POOL"
}
```

| **Expected** | Wallet `id` → `<walletId>` |
| **Inspect** | `GET /api/v1/wallets/<walletId>/balance` → `availableBalance` |
| **Troubleshoot** | Family authz 403 if `FAMILY_AUTH_ENABLED` and membership missing |

For MEMBER_WALLET (allowance destination), create another wallet with `"walletType": "MEMBER_WALLET"` and the child’s `ownerMemberId`.

---

### Step 5 — FakePayment top-up (core money demo)

| | |
|--|--|
| **Purpose** | Provider confirm → Wallet credit |
| **Create** | `POST /api/v1/payments` (`CreatePaymentRequest`) |

```json
{
  "familyId": "<familyId>",
  "payerMemberId": "<memberId>",
  "walletId": "<walletId>",
  "amount": 100000,
  "purpose": "Demo top-up",
  "providerCode": "FAKE_PAYMENT"
}
```

| **Expected** | Response with payment order id (`paymentOrderId` or `id`) |
| **IPN** | `POST /api/v1/payments/fake/ipn` (`FakePaymentIpnRequest`) |

```json
{ "orderId": "<orderId>" }
```

| **Expected** | `resultCode` success; **repeat IPN** → still one credit |
| **Inspect** | Balance increases by 100000; ledger via `GET /api/v1/wallets/<walletId>/transactions` and/or `GET /api/v1/ledger/transactions/<txId>` |
| **Events** | `PaymentProviderPaid` → `WalletCreditPosted` (`PAYMENT_TOPUP`) |
| **Shortcut** | `./scripts/runtime-e2e-smoke.sh` |

---

### Step 6 — Ledger balanced posting

| | |
|--|--|
| **Purpose** | Show Σ DEBIT = Σ CREDIT |
| **Inspect** | Transaction payload / DB `ledger_entries` for the credit TX |
| **Illustrative** | DEBIT PSP_CLEARING 100000 / CREDIT WALLET_LIABILITY 100000 |

---

### Step 7 — Allowance (honest partial demo)

| | |
|--|--|
| **Purpose** | Show Budget request/approve and outbox intent |
| **Create** | `POST /api/v1/allowances/requests` |

```json
{
  "familyId": "<familyId>",
  "requesterMemberId": "<memberId>",
  "amount": 50000,
  "reason": "Weekly pocket money"
}
```

| **Approve** | `POST /api/v1/allowances/requests/<id>/approve` |

```json
{ "approverMemberId": "<ownerMemberId>", "comment": "OK" }
```

| **Expected** | Status moves toward pending disbursement; outbox `AllowanceDisbursementRequested` |
| **Limitation** | Wallet has `WalletApplicationService.disburseAllowance` but **no** `@KafkaListener` consumer for that event — automatic transfer via Kafka is **not** demoable until that listener exists |
| **Do not claim** | “Money moved to member wallet” unless you call an internal/test path you control |

---

### Step 8 — Expense wallet settlement

| | |
|--|--|
| **Purpose** | External spend from pool |
| **Create** | `POST /api/v1/expenses` (`CreateExpenseRequest`) |

```json
{
  "familyId": "<familyId>",
  "paidByMemberId": "<memberId>",
  "title": "Groceries",
  "amountTotal": 30000,
  "expenseDate": "2026-11-20",
  "notes": "Demo"
}
```

| **Pay** | `POST /api/v1/expenses/<expenseId>/submit-payment` |

```json
{ "walletId": "<walletId>" }
```

| **Expected** | Expense `PENDING_WALLET` then `PAID` after `WalletDebitPosted` (`EXPENSE_PAYMENT`) |
| **Failed path** | Insufficient funds → `ExpensePaymentFailed` → expense `FAILED` (no realized Analytics expense) |
| **Requires** | `KAFKA_ENABLED=true`, wallet ACTIVE, same family |

---

### Step 9 — Inventory low-stock notification

| | |
|--|--|
| **Purpose** | Household → Notification |
| **Seed item** (dev Flyway V99 on seed family) | id `14000000-0000-4000-8000-000000000011` (milk, reorder 6) |
| **List** | `GET /api/v1/household/inventory?familyId=<seedFamilyId>` |
| **Adjust** | `POST /api/v1/household/inventory/<inventoryItemId>/movements` |

```json
{
  "movementType": "OUTBOUND",
  "quantityDelta": -3,
  "reason": "Demo consumption"
}
```

| **Expected** | When on-hand ≤ reorder → outbox `InventoryLow` → notifications for OWNER/PARENT |
| **Inspect** | `GET /api/v1/notifications?...` (see controller query params) |
| **Note** | New demo families may lack catalog/inventory rows — use seed family or insert data first |

---

### Step 10 — Analytics realized totals

| | |
|--|--|
| **Purpose** | Income/expense from ledger only |
| **Endpoint** | `GET /api/v1/analytics/reports?familyId=<familyId>` |
| **Expected** | Income reflects `WalletCreditPosted` / `PAYMENT_TOPUP`; expense reflects `WalletDebitPosted` / `EXPENSE_PAYMENT` |
| **Not counted** | `PaymentProviderPaid`, `ExpensePaymentRequested`, allowance transfers |
| **Doc** | [analytics-metrics.md](../architecture/analytics-metrics.md) |

---

### Step 11 — Observability (optional)

```bash
docker compose --profile full --profile observability up -d
```

| | |
|--|--|
| **Prometheus** | http://localhost:9090 |
| **Grafana** | http://localhost:3000 (admin from `.env`) |
| **Tempo** | http://localhost:3200 |
| **Expectation** | Actuator metrics scraped; traces if OTLP path healthy |

---

## Timing guide

| Minutes | Focus |
|---------|--------|
| 0–2 | Stack + JWT + family/wallets |
| 2–6 | FakePayment top-up + ledger |
| 6–8 | Expense settlement **or** smoke script recap |
| 8–10 | Analytics + honest allowance caveat |
| 10–12 | Inventory/notification or Grafana |

## Cleanup

Prefer leaving demo families in place. If you must reset: stop compose, **explicitly** confirm before `docker compose down -v` (destructive).
