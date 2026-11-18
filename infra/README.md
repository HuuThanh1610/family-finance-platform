# Local infrastructure (Docker Compose)

## Profiles

| Profile | Nội dung |
|---------|----------|
| *(default)* | `postgres` |
| `full` | Postgres, Redis, Kafka, Keycloak, Mailpit, **toàn bộ microservice**, API Gateway, frontend (nginx) |
| `observability` | Prometheus, Grafana (dashboard Spring Boot), OpenTelemetry Collector |
| `web` | Frontend Vite dev (bind-mount, hot reload) |

## Khởi động

```bash
cp .env.example .env

# Chỉ DB (Flyway chạy khi start từng service)
docker compose up -d postgres

# Hạ tầng + ứng dụng container
docker compose --profile full up -d --build

# Thêm monitoring (cần `full` để Prometheus scrape được target trong mạng compose)
docker compose --profile full --profile observability up -d

# Dev frontend (tuỳ chọn, thay cho service `frontend` trong profile full)
docker compose --profile web up -d
```

URLs mặc định: Postgres `5432`, Gateway `8080`, Web `5173` (map tới nginx port 80), Keycloak `8180`, Mailpit UI `8025`, Prometheus `9090`, Grafana `3000`, OTLP gRPC `4317`.

## Dừng / gỡ

```bash
docker compose --profile full --profile observability --profile web down

# Xoá volume Postgres (chỉ khi cần bootstrap lại init SQL — không ảnh hưởng Flyway trong JAR)
docker compose down -v
```

## Ghi chú

- Kafka: client trong compose dùng `kafka:29092`; từ host dùng `localhost:9092` (listener `EXTERNAL`).
- `KAFKA_ENABLED=false` mặc định — bật trong `.env` khi test saga.
- Image Spring build multi-stage từ `services/<service>/Dockerfile`; không chỉnh Flyway migrations.
- Grafana dashboard dùng metric chuẩn Micrometer/Spring Boot (`http_server_requests_seconds_count`, `jvm_memory_used_bytes`, …).
- OTLP Collector (distroless): readiness `curl -s http://localhost:13133/` từ host khi port được publish (tuỳ chọn map `13133:13133`).

Chi tiết: [postgres/README.md](postgres/README.md), [keycloak/README.md](keycloak/README.md).