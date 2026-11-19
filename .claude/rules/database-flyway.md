---
paths:
  - "services/**/src/main/resources/db/migration/**/*.sql"
  - "services/**/src/main/resources/application.yml"
---

# Database / Flyway

- Schema changes only via Flyway under that service’s `db/migration/`. Append-only: never edit/delete applied migrations or rewrite history.
- Next version = max existing in **that** service + 1 (note V99 seeds and V100+ fixes already present). Descriptive names: `V###__short_snake.sql`.
- Keep `spring.jpa.hibernate.ddl-auto: validate`. Never `update`/`create` in service config.
- Match PostgreSQL types to existing JPA entities. Prefer expand → backfill → constrain for risky NOT NULL/type changes.
- No cross-service foreign keys. Migrations must replay on a clean DB for that service.
- Do not baseline/reset shared Docker volumes as part of routine edits.
