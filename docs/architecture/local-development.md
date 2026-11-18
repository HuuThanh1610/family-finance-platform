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

Mở http://localhost:5173 — đăng nhập demo bất kỳ email/password (≥6 ký tự).

`VITE_USE_MOCK=true` → dữ liệu mock, không cần backend.

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
