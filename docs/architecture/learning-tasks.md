# BACKEND ENGINEERING LEARNING TASKS

Các chủ đề dưới đây **cố ý chưa implement**. Schema/DTO/interface đã chuẩn bị để owner tự học.

## 1. Double-entry accounting
- **Mục đích:** Ghi sổ bất biến, Σ debit = Σ credit.
- **Files:** `services/wallet-service/.../db/migration/V2__*`, entity `Ledger*`, TODO `LedgerPostingService`.
- **AC:** Mỗi transfer tạo ≥2 entries; query kiểm tra cân bằng theo transaction_id.

## 2. Transaction isolation
- **Mục đích:** Tránh lost update khi cập nhật số dư.
- **Files:** `wallet_balances.version`, service transfer.
- **AC:** Hai transfer song song không làm âm số dư sai; isolation level được document.

## 3. Concurrent transfer handling
- **Mục đích:** Optimistic (`@Version`) hoặc pessimistic (`SELECT FOR UPDATE`).
- **Files:** `WalletBalanceRepository`, transfer use-case.
- **AC:** Load test 50 concurrent transfers giữ invariant ledger.

## 4. Idempotency
- **Mục đích:** Retry an toàn cho payment/transfer.
- **Files:** `idempotency_records`, unique `idempotency_key` trên ledger/payment.
- **AC:** Gửi cùng key 2 lần chỉ tạo 1 nghiệp vụ.

## 5. Outbox / Inbox
- **Mục đích:** Reliable messaging + dedupe consumer.
- **Files:** `outbox_events`, `inbox_events` migrations; publisher/poller TODO.
- **AC:** Event publish trong cùng DB transaction với nghiệp vụ; consumer bỏ qua duplicate.

## 6. Kafka event processing
- **Mục đích:** Cross-service integration.
- **Files:** `application.yml` kafka placeholders; `contracts/events/`.
- **AC:** Consumer idempotent; DLQ documented.

## 7. Saga
- **Mục đích:** Orchestrate payment → wallet credit → notification.
- **Files:** payment/wallet/notification services.
- **AC:** Compensation path khi credit thất bại sau payment success.

## 8. MoMo Sandbox
- **Mục đích:** Tích hợp cổng thanh toán thật (sandbox).
- **Files:** `PaymentProvider`, payment-service config placeholders.
- **AC:** Create order + redirect URL hoạt động với credential từ env (không hardcode).

## 9. Webhook security
- **Mục đích:** Xác thực chữ ký callback.
- **Files:** `webhook_inbox`, payment controller skeleton.
- **AC:** Reject payload sai chữ ký; lưu raw + hash; chống replay.

## 10. Resource-level authorization
- **Mục đích:** Chỉ member của family mới đọc/ghi resource.
- **Files:** Keycloak placeholder, Security config TODO.
- **AC:** Không tin `familyId` từ body; resolve từ JWT + membership.

## 11. Integration testing with Testcontainers
- **Mục đích:** Test DB/Kafka thật trong CI.
- **Files:** `src/test` README scenarios.
- **AC:** Transfer + payment webhook tests xanh với Postgres container.

## 12. Observability
- **Mục đích:** Metrics/traces/logs cho money path.
- **Files:** Actuator foundation.
- **AC:** TraceId xuyên request; metrics latency/error cho transfer & payment.
