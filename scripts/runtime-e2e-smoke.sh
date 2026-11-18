#!/usr/bin/env bash
# Runtime smoke: FakePaymentAdapter → PaymentProviderPaid → wallet credit (Kafka).
# Requires: curl, jq, stack up with `docker compose --profile full` and KAFKA_ENABLED=true.
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
REALM="${KEYCLOAK_REALM:-family-finance}"
SECURITY_ENABLED="${SECURITY_ENABLED:-false}"
TOPUP_AMOUNT="${TOPUP_AMOUNT:-50000}"
POLL_SECONDS="${POLL_SECONDS:-90}"

# Dev seed (Flyway V99) — used when SECURITY_ENABLED=false
SEED_PROFILE_ID="${SEED_PROFILE_ID:-b1000000-0000-4000-8000-000000000001}"
SEED_FAMILY_ID="${SEED_FAMILY_ID:-a1000000-0000-4000-8000-000000000001}"
SEED_MEMBER_ID="${SEED_MEMBER_ID:-c1000000-0000-4000-8000-000000000001}"
SEED_WALLET_ID="${SEED_WALLET_ID:-d1000000-0000-4000-8000-000000000001}"

KEYCLOAK_TOKEN_CLIENT="${KEYCLOAK_TOKEN_CLIENT:-family-finance-services}"
KEYCLOAK_CLIENT_SECRET="${KEYCLOAK_CLIENT_SECRET:-change-me-services-client-secret}"
DEMO_USERNAME="${DEMO_USERNAME:-owner.demo}"
DEMO_PASSWORD="${DEMO_PASSWORD:-ChangeMeOwner1!}"

log() { printf '==> %s\n' "$*"; }
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

need() { command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"; }
need curl
need jq

auth_headers=()
if [[ "${SECURITY_ENABLED}" == "true" ]]; then
  log "Lấy token Keycloak (password grant, client ${KEYCLOAK_TOKEN_CLIENT})"
  log "Lưu ý: family-finance-web chỉ PKCE — không dùng password grant; dùng family-finance-services."
  token_response="$(
    curl -fsS -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
      -H 'Content-Type: application/x-www-form-urlencoded' \
      -d "grant_type=password" \
      -d "client_id=${KEYCLOAK_TOKEN_CLIENT}" \
      -d "client_secret=${KEYCLOAK_CLIENT_SECRET}" \
      -d "username=${DEMO_USERNAME}" \
      -d "password=${DEMO_PASSWORD}"
  )"
  ACCESS_TOKEN="$(echo "${token_response}" | jq -r '.access_token // empty')"
  [[ -n "${ACCESS_TOKEN}" ]] || fail "Không lấy được access_token từ Keycloak"
  auth_headers=(-H "Authorization: Bearer ${ACCESS_TOKEN}")

  log "Kiểm tra API ẩn danh → kỳ vọng 401"
  anon_code="$(curl -s -o /dev/null -w '%{http_code}' "${GATEWAY_URL}/api/families")"
  [[ "${anon_code}" == "401" ]] || fail "Kỳ vọng 401 khi không JWT, nhận ${anon_code}"
else
  log "SECURITY_ENABLED=false — dùng seed V99 + header X-User-Id"
  auth_headers=(-H "X-User-Id: ${SEED_PROFILE_ID}")
fi

log "Gateway health"
curl -fsS "${GATEWAY_URL}/actuator/health" | jq -e '.status == "UP"' >/dev/null

family_id=""
member_id=""
wallet_id=""

jwt_sub() {
  python3 -c 'import json,base64,sys
t=sys.argv[1].split(".")[1]
t+="="*((4-len(t)%4)%4)
print(json.loads(base64.urlsafe_b64decode(t))["sub"])' "$1"
}

if [[ "${SECURITY_ENABLED}" == "true" ]]; then
  kc_sub="$(jwt_sub "${ACCESS_TOKEN}")"
  [[ -n "${kc_sub}" && "${kc_sub}" != "null" ]] || fail "Không đọc được sub từ JWT"

  log "Provision profile"
  curl -fsS -X POST "${GATEWAY_URL}/api/v1/profiles/provision" \
    "${auth_headers[@]}" -H 'Content-Type: application/json' \
    -d "$(jq -nc --arg sub "${kc_sub}" --arg email "${DEMO_USERNAME}@example.local" \
      '{keycloakSub: $sub, email: $email, displayName: "Owner Demo"}')" >/dev/null

  profile_id="$(curl -fsS "${GATEWAY_URL}/api/v1/profiles/me" "${auth_headers[@]}" | jq -r '.id')"
  [[ -n "${profile_id}" ]] || fail "Không lấy được profile id"

  log "Tạo family mới"
  smoke_slug="e2e-smoke-$(date +%s)"
  family_id="$(curl -fsS -X POST "${GATEWAY_URL}/api/families" \
    "${auth_headers[@]}" -H 'Content-Type: application/json' \
    -d "$(jq -nc --arg s "${smoke_slug}" '{name:"E2E Smoke Family",slug:$s,description:"runtime-e2e-smoke.sh"}')" | jq -r '.id')"
  member_id="$(curl -fsS "${GATEWAY_URL}/api/families/${family_id}/members" "${auth_headers[@]}" | jq -r '.[0].id')"
  [[ -n "${member_id}" ]] || fail "Không lấy được member id"

  log "Tạo ví FAMILY_POOL"
  wallet_id="$(curl -fsS -X POST "${GATEWAY_URL}/api/v1/wallets" \
    "${auth_headers[@]}" -H 'Content-Type: application/json' \
    -d "$(jq -nc --arg fid "${family_id}" --arg mid "${member_id}" \
      '{familyId: $fid, ownerMemberId: $mid, name: "E2E Pool", walletType: "FAMILY_POOL"}')" | jq -r '.id')"
else
  family_id="${SEED_FAMILY_ID}"
  member_id="${SEED_MEMBER_ID}"
  wallet_id="${SEED_WALLET_ID}"
fi

[[ -n "${wallet_id}" ]] || fail "wallet_id trống"

balance_before="$(curl -fsS "${GATEWAY_URL}/api/v1/wallets/${wallet_id}/balance" \
  "${auth_headers[@]}" | jq -r '.availableBalance')"
log "Số dư trước: ${balance_before}"

log "Tạo payment (FAKE_PAYMENT)"
payment_json="$(curl -fsS -X POST "${GATEWAY_URL}/api/v1/payments" \
  "${auth_headers[@]}" -H 'Content-Type: application/json' \
  -d "$(jq -nc \
    --arg fid "${family_id}" \
    --arg mid "${member_id}" \
    --arg wid "${wallet_id}" \
    --argjson amt "${TOPUP_AMOUNT}" \
    '{familyId: $fid, payerMemberId: $mid, walletId: $wid, amount: $amt, purpose: "E2E top-up", providerCode: "FAKE_PAYMENT"}')")"
order_id="$(echo "${payment_json}" | jq -r '.paymentOrderId // .id')"
[[ -n "${order_id}" && "${order_id}" != "null" ]] || fail "Không tạo được payment order: ${payment_json}"
log "Payment order: ${order_id}"

log "Giả lập callback provider (2 lần — idempotent)"
for _ in 1 2; do
  curl -fsS -X POST "${GATEWAY_URL}/api/v1/payments/fake/ipn" \
    -H 'Content-Type: application/json' \
    -d "$(jq -nc --arg oid "${order_id}" '{orderId: $oid}')" >/dev/null
done

expected_after=$((balance_before + TOPUP_AMOUNT))
deadline=$((SECONDS + POLL_SECONDS))
balance_after="${balance_before}"
while (( SECONDS < deadline )); do
  balance_after="$(curl -fsS "${GATEWAY_URL}/api/v1/wallets/${wallet_id}/balance" \
    "${auth_headers[@]}" | jq -r '.availableBalance')"
  if [[ "${balance_after}" == "${expected_after}" ]]; then
    break
  fi
  sleep 2
done

[[ "${balance_after}" == "${expected_after}" ]] || fail \
  "Số dư sau ${balance_after} != kỳ vọng ${expected_after} (Kafka/outbox consumer chưa credit?)"

log "OK — một lần credit ${TOPUP_AMOUNT} VND (trước=${balance_before}, sau=${balance_after})"