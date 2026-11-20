# Architecture Overview

Family Finance is a monorepo of microservices with **Database-per-Service**.

> Prefer [docs/portfolio/ARCHITECTURE_WALKTHROUGH.md](../portfolio/ARCHITECTURE_WALKTHROUGH.md) and verified source when older notes conflict.

## Bounded contexts

| Context | Service | Database |
|---------|---------|----------|
| Profile | profile-service | profile_db |
| Family | family-service | family_db |
| Wallet / Ledger | wallet-service | wallet_db |
| Payment | payment-service | payment_db |
| Budget & Allowance | budget-service | budget_db |
| Expense | expense-service | expense_db |
| Household | household-service | household_db |
| Notification | notification-service | notification_db |
| Analytics | analytics-service | analytics_db |

## Principles

- No foreign keys across databases.
- Sync via REST; async via domain events (Kafka) with transactional Outbox/Inbox.
- VND amounts as `BIGINT`; distributed ids as `UUID`.
- Web UI talks to API Gateway (`VITE_USE_MOCK=false` by default in `.env.example`).
- Wallet Service alone posts the ledger and owns balance projections.

## C4 — System Context

```mermaid
C4Context
title Family Finance — System Context
Person(member, "Family member", "Wallets, budgets, expenses")
System(ff, "Family Finance Platform", "Web + microservices (portfolio/demo)")
System_Ext(momo, "MoMo Sandbox", "Optional payment adapter (not production)")
System_Ext(fake, "FakePayment", "Deterministic local provider")
System_Ext(keycloak, "Keycloak", "OIDC Identity Provider")
Rel(member, ff, "Uses Web UI / API Gateway")
Rel(ff, keycloak, "Validates JWT (Resource Server)")
Rel(ff, momo, "Create order / IPN (sandbox credentials)")
Rel(ff, fake, "Create order / fake IPN (local demo)")
```

## C4 — Containers

```mermaid
C4Container
title Family Finance — Containers
Person(user, "User")
Container(web, "Web App", "React/Vite", "UI via Gateway")
Container(gw, "api-gateway", "Spring Cloud Gateway", "JWT + routing")
Container(family, "family-service", "Spring Boot", "Members & permissions")
Container(wallet, "wallet-service", "Spring Boot", "Wallets & double-entry ledger")
Container(payment, "payment-service", "Spring Boot", "Top-up / provider abstraction")
Container(budget, "budget-service", "Spring Boot", "Budgets & allowance")
ContainerDb(pg, "PostgreSQL", "9 databases")
Container_Ext(kc, "Keycloak")
Container_Ext(kafka, "Kafka")
Rel(user, web, "HTTPS/local")
Rel(web, gw, "REST")
Rel(gw, family, "REST")
Rel(gw, wallet, "REST")
Rel(gw, payment, "REST")
Rel(gw, budget, "REST")
Rel(family, pg, "JDBC")
Rel(wallet, pg, "JDBC")
Rel(payment, pg, "JDBC")
Rel(budget, pg, "JDBC")
Rel(gw, kc, "JWT validate")
Rel(wallet, kafka, "Outbox events")
Rel(payment, kafka, "Outbox events")
```

## Bounded Context Map

```mermaid
flowchart LR
  Profile -.-> Family
  Family --> Wallet
  Family --> Budget
  Payment --> Wallet
  Budget --> Wallet
  Expense --> Wallet
  Household --> Notification
  Budget --> Notification
  Payment --> Notification
  Wallet --> Analytics
  Expense --> Analytics
  Household --> Analytics
```

Relations are **logical** (events/API), not FKs.

## Portfolio docs

- [README](../../README.md)
- [Demo scenario](../portfolio/DEMO_SCENARIO.md)
- [Evidence matrix](../portfolio/EVIDENCE_MATRIX.md)
- [Kafka contracts](../events/KAFKA_CONTRACTS.md)
- [Analytics metrics](analytics-metrics.md)
