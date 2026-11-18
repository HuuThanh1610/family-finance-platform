# Integration test scaffolding (Testcontainers)

Không có test giả `assertTrue` trong scaffold. Thêm module test theo từng service khi sẵn sàng.

## Kịch bản giá trị cao (gợi ý)

### profile-service
- Flyway migrate + đọc `user_profiles` seed (V99)
- Unique constraint `keycloak_sub` / email

### family-service
- RBAC seed `family_roles` / `family_permissions`
- Đọc members theo `family_id`

### wallet-service
- **Không** test CRUD số dư; chỉ đọc `wallet_accounts`
- TODO: double-entry invariant (DEBIT=CREDIT) khi `LedgerPostingService` được implement

### payment-service
- Đọc `payment_orders`; webhook inbox idempotency (khi có processor)

### budget-service
- Allowance request workflow + outbox row pending

### expense-service
- Chi phí theo `family_id` + category hierarchy

### household-service
- Shopping list + catalog SKU unique per family

### notification-service
- Template unique (key, locale, channel)

### analytics-service
- Upsert natural key `family_id + summary_month`

## Mẫu dependency (test scope)

```xml
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>
```

```java
// TODO: @SpringBootTest + @ServiceConnection hoặc @DynamicPropertySource — reserved learning topic
```
