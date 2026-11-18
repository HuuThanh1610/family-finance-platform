# Family Finance Platform

Nền tảng quản lý tài chính & hộ gia đình (ví nội bộ mô phỏng + MoMo Sandbox). Không phải ngân hàng / e-wallet regulated.

## Quick start

```bash
cp .env.example .env
docker compose up -d postgres

# optional: Kafka + Keycloak + Mailpit
docker compose --profile full up -d

cd frontend/web && cp .env.example .env && npm install && npm run dev
```

- Web: http://localhost:5173 (`VITE_USE_MOCK=true` mặc định)
- Gateway: `services/api-gateway` port 8080
- Tiến độ: [docs/implementation/PROGRESS.md](docs/implementation/PROGRESS.md)
- Báo cáo: [docs/implementation/FINAL_IMPLEMENTATION_REPORT.md](docs/implementation/FINAL_IMPLEMENTATION_REPORT.md)

## Services

| Service | Port | DB |
|---------|------|----|
| api-gateway | 8080 | — |
| profile-service | 8081 | profile_db |
| family-service | 8082 | family_db |
| wallet-service | 8083 | wallet_db |
| payment-service | 8084 | payment_db |
| budget-service | 8085 | budget_db |
| expense-service | 8086 | expense_db |
| household-service | 8087 | household_db |
| notification-service | 8088 | notification_db |
| analytics-service | 8089 | analytics_db |

```bash
mvn -f services/wallet-service/pom.xml test
mvn -f services/payment-service/pom.xml test
mvn -f services/wallet-service/pom.xml spring-boot:run
```

## Implemented highlights

- Double-entry ledger (VND BIGINT), transfers, payment top-up credit
- MoMo Sandbox adapter + HMAC IPN + FakePaymentAdapter (live sandbox cần credentials)
- Outbox/Inbox + payment top-up saga (Kafka optional via `KAFKA_ENABLED`)
- Family membership authorization, profile provision
- Budget/allowance saga states, expense wallet payment workflow, inventory low-stock outbox
- CI (GitHub Actions), Keycloak realm import, K8s starter manifests

## Docs

- [Payment MoMo Sandbox](docs/payment/MOMO_SANDBOX.md)
- [Kafka contracts](docs/events/KAFKA_CONTRACTS.md)
- [Top-up saga](docs/workflows/PAYMENT_TOPUP_SAGA.md)
- [Keycloak](infra/keycloak/README.md)
- [Kubernetes](infra/k8s/README.md)
- [Local development](docs/architecture/local-development.md)
