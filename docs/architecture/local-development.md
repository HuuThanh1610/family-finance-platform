# Local Development

## Prerequisites

- Node.js 22+
- Docker Desktop
- Java 21 + Maven 3.9+ (cho backend)

## 1. Infrastructure

```bash
cp .env.example .env
docker compose up -d postgres
```

Verify databases:

```bash
docker compose exec postgres psql -U postgres -c '\l'
```

Apply SQL migrations (dev check without Spring Boot):

```bash
chmod +x infra/postgres/apply-migrations.sh
# hoặc dùng vòng lặp psql như đã verify trong CI local
```

Optional full stack (containerized apps + gateway + frontend):

```bash
docker compose --profile full up -d --build
docker compose --profile full --profile observability up -d
```

Xem [infra/README.md](../../infra/README.md).

## 2. Frontend

```bash
cd frontend/web
cp .env.example .env
npm install
npm run dev
```

Mở http://localhost:5173. Với Compose `full` + Keycloak, dùng client PKCE / demo users trong [infra/keycloak/README.md](../../infra/keycloak/README.md).

`.env.example` đặt `VITE_USE_MOCK=false` (API thật qua Gateway). Đặt `VITE_USE_MOCK=true` chỉ khi muốn UI mock không cần backend.

Demo API walkthrough: [docs/portfolio/DEMO_SCENARIO.md](../portfolio/DEMO_SCENARIO.md).

## 3. Backend (per service)

```bash
mvn -f services/family-service/pom.xml spring-boot:run
```

Actuator: `http://localhost:8082/actuator/health`  
Swagger: `http://localhost:8082/swagger-ui.html`

## Notes

- `.env` không commit.
- MoMo credentials để trống cho đến khi owner triển khai sandbox.
- Không hardcode production secrets.
- Compose profile `full` bật `SECURITY_ENABLED` / `KAFKA_ENABLED` theo `infra/compose/apps.yml`; IDE `application.yml` vẫn mặc định `false` cho unit test.
- Smoke FakePayment→wallet: `SECURITY_ENABLED=true KAFKA_ENABLED=true ./scripts/runtime-e2e-smoke.sh`.
