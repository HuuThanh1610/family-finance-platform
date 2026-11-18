# Final Implementation Report

**Date:** 2026-11-18  
**Repo:** https://github.com/HuuThanh1610/family-finance-platform  
**Branch:** `main`  
**Session commits:** `0890e16` → `05a5318` (on top of prior `9300e4f`)

## 1. Implemented and verified

- Independent OAuth2 Resource Server JWT enforcement on every Spring service (gateway is not the only layer).
- Family membership / role / permission checks with cross-family denial unit tests.
- Wallet double-entry ledger with concurrent transfer IT against PostgreSQL (**9/9 PASS** via `WALLET_IT=true` + `WALLET_IT_JDBC_URL=jdbc:postgresql://localhost:5432/wallet_it_db`).
- Payment MoMo HMAC/IPN/Fake adapter + recovery (duplicate IPN, outbox retry, reconcile APIs) — **22/22 PASS**.
- Frontend Keycloak Authorization Code + PKCE; mock only when `VITE_USE_MOCK=true`; production build OK.
- Compose config valid; Dockerfiles for all apps; Prometheus/Grafana dashboards using real Micrometer names; OTel collector present.

## 2. Implemented but not externally verified

- Live MoMo sandbox checkout / IPN over public HTTPS.
- Full Kafka broker E2E (PaymentProviderPaid → WalletCreditPosted, allowance/expense/inventory sagas) with real broker assertions.
- Full `docker compose --profile full up --build` health of every app container.
- Kind/Kubernetes live deployment.
- Automatic Testcontainers container lifecycle on this Docker Desktop 29 host (API 400 from docker-java).

## 3. Not implemented / incomplete

- Spring apps exporting OTLP traces to the collector by default (collector ready; exporter wiring partial).
- Exhaustive Grafana panels for Kafka lag / Hikari pools beyond Spring HTTP/JVM basics.

## 4. Blocked by owner credentials

- MoMo partner/access/secret keys and reachable `MOMO_IPN_URL`.

## 5. Known technical limitations

- Docker Desktop 29 + Testcontainers/docker-java: use external JDBC fallback documented in wallet IT base class.
- Existing `wallet_db` may lack `flyway_schema_history` if previously created without Flyway; prefer clean DB or baseline carefully.
- Repo is PUBLIC — keep only placeholder secrets; Keycloak realm demo passwords are local-dev only.

## Evidence commands

```bash
# Security / unit
mvn -f services/family-service/pom.xml test
mvn -f services/payment-service/pom.xml test

# Wallet IT (compose Postgres)
docker exec ff-postgres psql -U postgres -c "CREATE DATABASE wallet_it_db OWNER wallet_user;"
WALLET_IT=true \
WALLET_IT_JDBC_URL=jdbc:postgresql://localhost:5432/wallet_it_db \
WALLET_IT_DB_USER=wallet_user \
WALLET_IT_DB_PASSWORD=changeme_wallet \
  mvn -f services/wallet-service/pom.xml test -Dtest=WalletConcurrentTransferIT,WalletLedgerIntegrityIT

# Frontend
cd frontend/web && npm run build

# Compose
docker compose config -q
```

**Definition of Done:** critical local verification for JWT, wallet concurrency (external PG), payment IPN tests, frontend OIDC wiring, and compose config are met. Live MoMo + full Kafka broker + full compose image bring-up remain explicitly unverified/blocked — project is **not** claimed fully complete for those external items.
