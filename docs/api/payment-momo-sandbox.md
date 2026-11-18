# MoMo Sandbox — payment-service

## Env

| Variable | Mô tả |
|----------|--------|
| `MOMO_ENVIRONMENT` | `sandbox` (gợi ý) |
| `MOMO_PARTNER_CODE` | Partner code từ MoMo |
| `MOMO_ACCESS_KEY` | Access key |
| `MOMO_SECRET_KEY` | Secret key (HMAC) |
| `MOMO_BASE_URL` | Mặc định `https://test-payment.momo.vn` |
| `MOMO_REDIRECT_URL` | URL sau khi thanh toán |
| `MOMO_IPN_URL` | URL webhook IPN (public) |

Thiếu credential → `POST /api/v1/payments` với `providerCode=MOMO_SANDBOX` trả lỗi cấu hình (không giả lập thành công).

## API

- `POST /api/v1/payments` — tạo đơn + gọi MoMo create
- `POST /api/v1/payments/momo/ipn` — IPN (ký HMAC AIOv2)
- `POST /api/v1/internal/payments/{id}/mark-completed` — sau khi wallet credit

## IPN / tunnel

MoMo sandbox gọi IPN từ internet. Dev local cần tunnel (ngrok, Cloudflare Tunnel, …) trỏ tới `http://localhost:8084/api/v1/payments/momo/ipn` và set `MOMO_IPN_URL` đúng URL public.

## Kiểm thử

- Unit test: `MomoSignatureService`, `MomoIpnService`, `FakePaymentAdapter`
- Provider giả: `providerCode=FAKE_PAYMENT` (không cần MoMo)

## Trạng thái live sandbox

Xác minh end-to-end với MoMo sandbox **đang chờ credential owner** — adapter và ký IPN đã implement; chưa chạy thanh toán thật trên sandbox khi chưa có key.