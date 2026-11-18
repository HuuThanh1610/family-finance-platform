# Schema overview (DB-per-service)

Tham chiếu UUID giữa các service là **logic** (không có FK xuyên database). Demo dev dùng cùng bộ ID cho **Gia đình Nguyễn** (`family_id = a1000000-…-0001`).

## profile_db

| Bảng | Ghi chú |
|------|---------|
| `user_profiles` | PK UUID, `keycloak_sub` UNIQUE |
| `user_preferences` | JSONB theo key |
| `user_consents` | Versioned consent |

**Invariant:** email và keycloak_sub duy nhất trên profile đang ACTIVE.

## family_db

| Bảng | Ghi chú |
|------|---------|
| `families` | `slug` UNIQUE |
| `family_members` | `role_code` ∈ OWNER, PARENT, ADULT_MEMBER, CHILD, VIEWER |
| `family_invitations` | `invite_token` UNIQUE |
| `family_roles` / `family_permissions` | RBAC cục bộ |
| `family_audit_logs` | Không `updated_at` |

## wallet_db (double-entry)

| Bảng | Ghi chú |
|------|---------|
| `wallet_accounts` | Không cột balance — số dư derived |
| `ledger_accounts` | Chart of accounts per ví |
| `ledger_transactions` | `reference_id`, `idempotency_key` UNIQUE |
| `ledger_entries` | `amount > 0`, `DEBIT`/`CREDIT`, **không** `updated_at` |
| `wallet_balances` | Snapshot + `version` optimistic lock |
| `idempotency_records` | Chống ghi trùng |
| `inbox_events` / `outbox_events` | Integration |

**Invariants (kế toán):**

1. Mỗi `ledger_transaction` phải có **tổng DEBIT = tổng CREDIT** (enforce application).
2. `ledger_entries.amount` luôn dương; chiều ghi sổ = `entry_type`.
3. Không cập nhật/xóa `ledger_entries`; điều chỉnh = bút toán đảo.
4. `wallet_balances` chỉ cập nhật qua domain service (không CRUD patch số dư).

## payment_db

`payment_orders` → `payment_attempts` → `provider_transactions`; `webhook_inbox` idempotent; `payment_refunds`, `payment_reconciliations`; `outbox_events`.

## budget_db

`budgets` / `budget_categories`; `allowance_policies`; `allowance_requests` + `approval_history`; `outbox_events`.

## expense_db

`expense_categories`, `expenses`, `expense_items`, `expense_attachments`. Số tiền VND: `BIGINT`.

## household_db

`item_catalog`, `inventory_items`, `inventory_movements`, `shopping_lists`, `shopping_list_items`, `shopping_assignments`.

## notification_db

`notification_templates`, `notifications`, `notification_deliveries`, `notification_preferences`.

## analytics_db

Read models: `family_monthly_summary`, `member_spending_summary`, `category_spending_summary`, `inventory_summary` — upsert theo khóa tự nhiên (family + kỳ).

## Kiểu dữ liệu chung

- PK: `UUID` (`gen_random_uuid()`)
- Tiền VND: `BIGINT`
- Thời gian: `TIMESTAMPTZ` (UTC)
- `created_at` / `updated_at` trừ bảng audit/ledger bất biến