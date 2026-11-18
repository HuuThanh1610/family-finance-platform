# Architecture Overview

Family Finance là monorepo microservices theo **Database-per-Service**.

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

- Không có foreign key xuyên database.
- Giao tiếp đồng bộ qua REST; bất đồng bộ qua domain events (Kafka) — schema outbox/inbox đã chuẩn bị.
- Số tiền VND dùng `BIGINT`; identity phân tán dùng `UUID`.
- Web UI chạy mock adapters cho đến khi API sẵn sàng.

## C4 — System Context

```mermaid
C4Context
title Family Finance — System Context
Person(member, "Thành viên gia đình", "Quản lý ví, ngân sách, chi tiêu")
System(ff, "Family Finance Platform", "Web + microservices quản lý tài chính hộ gia đình")
System_Ext(momo, "MoMo Sandbox", "Cổng thanh toán (chưa kết nối)")
System_Ext(keycloak, "Keycloak", "Identity Provider OIDC")
Rel(member, ff, "Sử dụng qua Web UI")
Rel(ff, keycloak, "Xác thực JWT (planned)")
Rel(ff, momo, "Tạo đơn / webhook (planned)")
```

## C4 — Containers

```mermaid
C4Container
title Family Finance — Containers
Person(user, "User")
Container(web, "Web App", "React/Vite", "UI tiếng Việt, mock API adapters")
Container(family, "family-service", "Spring Boot", "Thành viên & vai trò")
Container(wallet, "wallet-service", "Spring Boot", "Ví & double-entry ledger")
Container(payment, "payment-service", "Spring Boot", "Top-up / provider abstraction")
Container(budget, "budget-service", "Spring Boot", "Ngân sách & allowance")
ContainerDb(pg, "PostgreSQL", "9 databases")
Container_Ext(kc, "Keycloak")
Container_Ext(kafka, "Kafka")
Rel(user, web, "HTTPS")
Rel(web, family, "REST")
Rel(web, wallet, "REST")
Rel(web, payment, "REST")
Rel(web, budget, "REST")
Rel(family, pg, "JDBC")
Rel(wallet, pg, "JDBC")
Rel(payment, pg, "JDBC")
Rel(budget, pg, "JDBC")
Rel(family, kc, "JWT validate")
Rel(wallet, kafka, "Outbox events")
```

## Bounded Context Map

```mermaid
flowchart LR
  Profile[Profile] -.-> Family
  Family --> Wallet
  Family --> Budget
  Wallet --> Payment
  Budget --> Wallet
  Expense --> Wallet
  Expense --> Budget
  Household --> Notification
  Budget --> Notification
  Payment --> Notification
  Wallet --> Analytics
  Expense --> Analytics
  Household --> Analytics
```

Quan hệ là **logical** (events/API), không phải FK.
