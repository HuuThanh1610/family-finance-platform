# MoMo Sandbox Integration Guide

## Scope

Internal virtual wallet top-up via **MoMo All-In-One (AIO) v2** sandbox.

Simulated bookkeeping balances only — not a real bank or regulated e-wallet.

## Environment variables

```bash
MOMO_ENVIRONMENT=SANDBOX
MOMO_PARTNER_CODE=
MOMO_ACCESS_KEY=
MOMO_SECRET_KEY=
MOMO_PUBLIC_KEY=          # optional for this captureWallet flow
MOMO_BASE_URL=https://test-payment.momo.vn
MOMO_REDIRECT_URL=https://<your-tunnel>/payments/return
MOMO_IPN_URL=https://<your-tunnel>/api/v1/payments/momo/ipn
```

Never commit credentials. Live sandbox verification is **pending owner credentials**.

## Local IPN tunnel

MoMo cannot call `localhost`. Use Cloudflare Tunnel or ngrok:

```bash
cloudflared tunnel --url http://localhost:8084
# set MOMO_IPN_URL to https://<tunnel>/api/v1/payments/momo/ipn
```

## Signature (create)

Canonical string (exact field order):

```text
accessKey=$accessKey&amount=$amount&extraData=$extraData&ipnUrl=$ipnUrl&orderId=$orderId&orderInfo=$orderInfo&partnerCode=$partnerCode&redirectUrl=$redirectUrl&requestId=$requestId&requestType=$requestType
```

`signature = HMAC-SHA256(secretKey, raw)` hex lowercase.

## Lifecycle

`CREATED` → `PENDING_PROVIDER` → `PROVIDER_PAID` → `CREDIT_PENDING` → `COMPLETED`

Provider success ≠ wallet credited. Wallet Service consumes `PaymentProviderPaid` idempotently.

## Automated tests without credentials

- `FakePaymentAdapter` (`providerCode=FAKE_PAYMENT`)
- `MoMoSandboxAdapter` + MockWebServer / WireMock
- Signature unit tests

## Official docs

https://developers.momo.vn/v3/vi/docs/payment/
