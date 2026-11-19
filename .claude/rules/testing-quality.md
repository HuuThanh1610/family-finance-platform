---
paths:
  - "services/**/src/test/**"
  - "frontend/web/**/*.{test,spec}.{ts,tsx}"
  - "scripts/**/*smoke*"
  - "scripts/**/*verify*"
---

# Testing

- Use JUnit/Mockito/Spring test styles already in the service; frontend follows existing lint/build gates.
- Unit tests: business rules and edge cases. Integration tests must hit real infra when the suite is designed for it (`WALLET_IT`, Kafka, etc.)—do not silently skip or replace with mocks to greenwash.
- Cover authz failures, idempotency, concurrency, and ledger balance when touching those areas. Add regression tests for bugs you fix.
- Deterministic fixtures; unique IDs; no flaky sleeps. Clean only test-owned data. No production credentials.
- Report exact commands and Tests run/fail/skip. Do not claim runtime E2E when only unit tests ran (`scripts/runtime-*-*.sh` vs Surefire).
