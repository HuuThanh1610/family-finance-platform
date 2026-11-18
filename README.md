# Family Finance Platform

Nền tảng quản lý tài chính & hộ gia đình (portfolio-grade monorepo).

## Cấu trúc

```text
family-finance/
├── frontend/web/          # React + Vite + TypeScript
├── services/              # Spring Boot microservices
├── contracts/             # OpenAPI & domain events
├── infra/                 # Docker, Postgres, Keycloak
└── docs/                  # Architecture, ERD, API, ADR
```

## Quick start

Xem hướng dẫn đầy đủ tại [docs/architecture/local-development.md](docs/architecture/local-development.md) (sẽ được bổ sung trong các commit tiếp theo).

```bash
# Infrastructure (PostgreSQL)
docker compose up -d postgres

# Frontend
cd frontend/web && npm install && npm run dev
```

## Tech stack

| Layer | Stack |
|-------|-------|
| Web | React, TypeScript, Vite, Tailwind, shadcn/ui |
| Backend | Java 21, Spring Boot, Maven |
| Data | PostgreSQL (DB-per-service), Flyway |
| Infra | Docker Compose, Keycloak (placeholder), Kafka (placeholder) |

## License

Private portfolio project.
