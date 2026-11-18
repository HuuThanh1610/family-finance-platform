# PostgreSQL (DB-per-service)

Mỗi microservice có database riêng, user riêng, và Flyway migrations trong `services/<service>/src/main/resources/db/migration/`.

## Databases

| Database | User | Service | Mô tả ngắn |
|----------|------|---------|------------|
| `profile_db` | `profile_user` | profile-service | Hồ sơ người dùng, tuỳ chọn, consent |
| `family_db` | `family_user` | family-service | Gia đình, thành viên, vai trò, audit |
| `wallet_db` | `wallet_user` | wallet-service | Ví, sổ cái kép, số dư (derived) |
| `payment_db` | `payment_user` | payment-service | Đơn thanh toán, provider, webhook |
| `budget_db` | `budget_user` | budget-service | Ngân sách, tiền tiêu vặt, phê duyệt |
| `expense_db` | `expense_user` | expense-service | Chi tiêu, hóa đơn, đính kèm |
| `household_db` | `household_user` | household-service | Kho đồ, danh sách mua sắm |
| `notification_db` | `notification_user` | notification-service | Thông báo, template, delivery |
| `analytics_db` | `analytics_user` | analytics-service | Bảng tổng hợp read-model |

## Khởi tạo

Script `init/01-create-databases.sql` được mount vào `docker-entrypoint-initdb.d` khi volume Postgres trống.

**Mật khẩu:** các giá trị `changeme_*` trong script là placeholder cho local dev. Đặt secret thật trong `.env` (không commit) và map sang:

- `POSTGRES_*` / per-service `SPRING_DATASOURCE_*`
- Docker Compose `env_file: .env`

## Kết nối local (ví dụ)

```bash
psql "postgresql://profile_user:changeme_profile@localhost:5432/profile_db"
```

## Flyway

Mỗi service chạy Flyway độc lập trên database của mình. Không có foreign key xuyên database; các `UUID` (family_id, user_profile_id, …) là tham chiếu logic qua sự kiện/API.

## Schema

Tổng quan bảng và invariant: [docs/database/schema-overview.md](../../docs/database/schema-overview.md).