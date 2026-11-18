# Family Finance — Backend Services

Mỗi microservice là **Maven module độc lập** (parent `spring-boot-starter-parent` riêng). Có thể build/chạy từng service không cần reactor.

## Yêu cầu

- Java 21
- Maven 3.9+
- PostgreSQL (xem `infra/postgres/init/01-create-databases.sql`)

## Build một service

```bash
mvn -q -f services/family-service/pom.xml -DskipTests package
```

## Chạy dev

```bash
export DB_URL=jdbc:postgresql://localhost:5432/family_db
export DB_USER=family_user
export DB_PASSWORD=changeme_family
mvn -f services/family-service/pom.xml spring-boot:run
```

Hoặc chỉ dùng default trong `application.yml` (localhost + user placeholder).

## Cổng mặc định

| Service | Port | Database |
|---------|------|----------|
| profile-service | 8081 | profile_db |
| family-service | 8082 | family_db |
| wallet-service | 8083 | wallet_db |
| payment-service | 8084 | payment_db |
| budget-service | 8085 | budget_db |
| expense-service | 8086 | expense_db |
| household-service | 8087 | household_db |
| notification-service | 8088 | notification_db |
| analytics-service | 8089 | analytics_db |

## OpenAPI

Swagger UI: `http://localhost:<port>/swagger-ui.html`

## Chủ đề để lại TODO (học sau)

- OAuth2 Resource Server (Keycloak)
- Double-entry `LedgerPostingService`
- MoMo / PSP signing & webhooks
- Outbox relay & Kafka consumers
- Integration/E2E tests (Testcontainers)
