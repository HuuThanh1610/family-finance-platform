# Family Finance Platform

Monorepo portfolio: quản lý tài chính & hộ gia đình (React + Spring Boot microservices + PostgreSQL DB-per-service).

## Quick start

```bash
cp .env.example .env
docker compose up -d postgres

cd frontend/web
cp .env.example .env
npm install
npm run dev
```

Web: http://localhost:5173 (mock auth + demo data, tiếng Việt, VND).

## Structure

```text
frontend/web/          React + Vite + TypeScript + Tailwind
services/*/            Spring Boot 3.3 / Java 21 microservices
contracts/             OpenAPI & domain events
infra/                 Postgres init, Keycloak placeholder
docs/                  Architecture, ERD, learning tasks
```

## Backend services

| Service | Port | DB |
|---------|------|----|
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
mvn -f services/family-service/pom.xml spring-boot:run
# health: http://localhost:8082/actuator/health
```

Chi tiết: [docs/architecture/local-development.md](docs/architecture/local-development.md)

## BACKEND ENGINEERING LEARNING TASKS

Các phần **cố ý chưa implement** (schema/TODO đã chuẩn bị):

- Double-entry accounting & concurrent transfers
- Idempotency, Outbox/Inbox, Kafka, Saga
- MoMo Sandbox + webhook security
- Keycloak JWT / resource-level authorization
- Testcontainers & observability

Xem: [docs/architecture/learning-tasks.md](docs/architecture/learning-tasks.md)

## Docs

- [Architecture & C4](docs/architecture/overview.md)
- [ERD](docs/database/erd.md)
- [API overview](docs/api/overview.md)
- [Sequences](docs/architecture/sequences.md)
