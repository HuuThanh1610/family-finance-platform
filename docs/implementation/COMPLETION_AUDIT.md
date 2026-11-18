# Completion Audit

**Date:** 2026-11-18 (runtime integration)  
**Baseline HEAD (earlier hardening):** `666cb0f`  
**Remote:** `https://github.com/HuuThanh1610/family-finance-platform.git` (`main`)

## Requirement matrix

| Requirement | Status | Evidence |
|-------------|--------|----------|
| Microservices independent | PASS | 9 services + gateway |
| Flyway forward-only | PASS | V100+ CHAR→VARCHAR only; no history rewrite |
| JWT Resource Server each service | PASS | Runtime 401 + unit SecurityFilterChain |
| Cross-family isolation | PASS | Runtime Keycloak JWT: non-member + seed-wallet cross-family → 403 |
| CHILD / VIEWER resource authz | PASS | `budget.approve` / `wallet.transfer` permission checks + runtime script |
| Wallet concurrent IT | PASS* | *external JDBC (`wallet_db`); Testcontainers BLOCKED on Docker Desktop 29 |
| Ledger integrity IT | PASS | 4/4 |
| MoMo HMAC + IPN + FakePayment tests | PASS | payment 24 tests |
| Live MoMo sandbox E2E | BLOCKED | credentials |
| Payment recovery paths | PASS | unit + Fake IPN idempotency (double callback → one credit) |
| Frontend Keycloak OIDC + PKCE | PASS | build OK; stack Keycloak healthy |
| Frontend real API default | PASS | `VITE_USE_MOCK` default false |
| Dockerfiles + full Compose | PASS | `--profile full --profile observability` healthy |
| Kafka E2E real broker | PASS | PaymentProviderPaid outbox→Kafka→wallet inbox PROCESSED; ledger balanced |
| Prometheus metrics | PASS | `/actuator/prometheus` on services |
| OpenTelemetry traces | PASS | OTLP→collector→Tempo; gateway+family on shared `traceparent` |
| K8s deploy | PARTIAL | manifests; no live deploy |
| Secrets in git | PASS | placeholders only |

## Runtime commands

```bash
docker compose --profile full --profile observability config -q
docker compose --profile full --profile observability up -d --build

SECURITY_ENABLED=true KAFKA_ENABLED=true ./scripts/runtime-e2e-smoke.sh
./scripts/runtime-security-verify.sh

WALLET_IT=true \
WALLET_IT_JDBC_URL=jdbc:postgresql://localhost:5432/wallet_db \
WALLET_IT_DB_USER=wallet_user \
WALLET_IT_DB_PASSWORD=changeme_wallet \
  mvn -f services/wallet-service/pom.xml test -Dtest='*IT'
```

## Secrets note

No production MoMo secrets committed. Demo Keycloak passwords are local-dev only. Do not log secret values.
