# Implementation Progress

**Updated:** 2026-11-18 (checkpoint 2)

**Remote:** push pending after this checkpoint commits

## Completed & verified

| Milestone | Verification |
|-----------|--------------|
| INITIAL_AUDIT | Source + command audit |
| Wallet double-entry ledger | `mvn test` wallet OK (3) |
| Family membership + authz | `mvn test` family OK (10) |
| Payment MoMo adapter + IPN + Fake | `mvn test` payment OK (13) |
| Profile provision/preferences/consents | `mvn test` profile OK |
| API Gateway scaffold | `mvn package` OK |
| Wallet Kafka outbox/inbox consumer | code in; Kafka default off |
| Keycloak realm import JSON | `infra/keycloak/family-finance-realm.json` |
| CI workflow | `.github/workflows/ci.yml` |

## Payment APIs

- `POST /api/v1/payments` (create + provider initiate, returns payUrl)
- `GET /api/v1/payments`, `GET /api/v1/payments/{id}`
- `POST /api/v1/payments/momo/ipn` (HMAC verify, dedupe, outbox `PaymentProviderPaid`)
- Providers: `MOMO_SANDBOX`, `FAKE_PAYMENT`
- Docs: `docs/payment/MOMO_SANDBOX.md`

## Profile APIs

- `POST /api/v1/profiles/provision`
- `GET/PUT /api/v1/profiles/me`
- preferences + consents under `/me/*`

## Remaining (Definition of Done not fully met)

1. Payment outbox Kafka publisher + WalletCreditPosted → COMPLETED saga consumer
2. Budget/allowance/expense recoverable workflows
3. Household / notification / analytics event consumers
4. JWT Resource Server enforcement on all services
5. Frontend `VITE_USE_MOCK=false` real API wiring
6. Testcontainers concurrency tests
7. Full Docker Compose service containers + observability + K8s
8. FINAL_IMPLEMENTATION_REPORT.md

## Known limitations

- MoMo live sandbox **not** verified (credentials pending)
- Kafka disabled by default (`KAFKA_ENABLED=false`)
- Frontend still defaults to mock
- JWT security still placeholder on most services
