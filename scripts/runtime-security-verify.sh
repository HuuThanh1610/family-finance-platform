#!/usr/bin/env bash
# Runtime security checks against a live full stack (JWT + family authz).
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
REALM="${KEYCLOAK_REALM:-family-finance}"
CLIENT_ID="${KEYCLOAK_TOKEN_CLIENT:-family-finance-services}"
CLIENT_SECRET="${KEYCLOAK_CLIENT_SECRET:-change-me-services-client-secret}"
SEED_FAMILY_ID="${SEED_FAMILY_ID:-a1000000-0000-4000-8000-000000000001}"
SEED_WALLET_ID="${SEED_WALLET_ID:-d1000000-0000-4000-8000-000000000001}"

need() { command -v "$1" >/dev/null 2>&1 || { echo "Missing $1" >&2; exit 1; }; }
need curl; need jq; need python3
log() { printf '==> %s\n' "$*"; }
fail() { printf 'FAIL: %s\n' "$*" >&2; exit 1; }
pass() { printf 'PASS: %s\n' "$*"; }

token() {
  local user="$1" pass="$2"
  curl -fsS -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d "grant_type=password&client_id=${CLIENT_ID}&client_secret=${CLIENT_SECRET}&username=${user}&password=${pass}" \
    | jq -r '.access_token // empty'
}

jwt_sub() {
  python3 -c 'import json,base64,sys
t=sys.argv[1].split(".")[1]; t+="="*((4-len(t)%4)%4)
print(json.loads(base64.urlsafe_b64decode(t))["sub"])' "$1"
}

code_only() {
  local method="$1" path="$2"; shift 2
  curl -s -o /dev/null -w '%{http_code}' -X "${method}" "${GATEWAY_URL}${path}" "$@"
}

OWNER_TOKEN="$(token owner.demo 'ChangeMeOwner1!')"
PARENT_TOKEN="$(token parent.demo 'ChangeMeParent1!')"
[[ -n "${OWNER_TOKEN}" && -n "${PARENT_TOKEN}" ]] || fail "Keycloak password grant"

log "Anonymous → 401"
[[ "$(code_only GET /api/families)" == "401" ]] || fail "anon families"
pass "anonymous 401"

log "Invalid JWT → 401"
[[ "$(code_only GET /api/families -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.invalid')" == "401" ]] || fail "invalid jwt"
pass "invalid JWT 401"

OWNER_SUB="$(jwt_sub "${OWNER_TOKEN}")"
PARENT_SUB="$(jwt_sub "${PARENT_TOKEN}")"

curl -fsS -X POST "${GATEWAY_URL}/api/v1/profiles/provision" \
  -H "Authorization: Bearer ${OWNER_TOKEN}" -H 'Content-Type: application/json' \
  -d "$(jq -nc --arg s "${OWNER_SUB}" '{keycloakSub:$s,email:"owner.demo@example.local",displayName:"Owner Demo"}')" >/dev/null
curl -fsS -X POST "${GATEWAY_URL}/api/v1/profiles/provision" \
  -H "Authorization: Bearer ${PARENT_TOKEN}" -H 'Content-Type: application/json' \
  -d "$(jq -nc --arg s "${PARENT_SUB}" '{keycloakSub:$s,email:"parent.demo@example.local",displayName:"Parent Demo"}')" >/dev/null

slug="sec-$(date +%s)"
FAMILY_JSON="$(curl -fsS -X POST "${GATEWAY_URL}/api/families" \
  -H "Authorization: Bearer ${OWNER_TOKEN}" -H 'Content-Type: application/json' \
  -d "$(jq -nc --arg s "${slug}" '{name:"Security Family",slug:$s,description:"runtime-security-verify"}')")"
FAMILY_ID="$(echo "${FAMILY_JSON}" | jq -r '.id')"
OWNER_MEMBER_ID="$(curl -fsS "${GATEWAY_URL}/api/families/${FAMILY_ID}/members" \
  -H "Authorization: Bearer ${OWNER_TOKEN}" | jq -r '.[0].id')"

WALLET_ID="$(curl -fsS -X POST "${GATEWAY_URL}/api/v1/wallets" \
  -H "Authorization: Bearer ${OWNER_TOKEN}" -H 'Content-Type: application/json' \
  -d "$(jq -nc --arg f "${FAMILY_ID}" --arg m "${OWNER_MEMBER_ID}" \
    '{familyId:$f,ownerMemberId:$m,name:"Sec Pool",walletType:"FAMILY_POOL"}')" | jq -r '.id')"

log "User without family membership → 403"
code="$(code_only GET "/api/v1/wallets/${WALLET_ID}/balance" -H "Authorization: Bearer ${PARENT_TOKEN}")"
[[ "${code}" == "403" ]] || fail "non-member balance expected 403 got ${code}"
pass "non-member 403"

log "Family A accessing Family B wallet → 403"
code="$(code_only GET "/api/v1/wallets/${SEED_WALLET_ID}/balance" -H "Authorization: Bearer ${OWNER_TOKEN}")"
[[ "${code}" == "403" ]] || fail "cross-family expected 403 got ${code}"
pass "cross-family 403"

# Attach parent as CHILD then attempt allowance approve
docker exec ff-postgres psql -U postgres -d family_db -v ON_ERROR_STOP=1 -c \
  "INSERT INTO family_members (id, family_id, user_profile_id, role_code, status, joined_at, created_at, updated_at)
   VALUES (gen_random_uuid(), '${FAMILY_ID}', '${PARENT_SUB}', 'CHILD', 'ACTIVE', NOW(), NOW(), NOW())
   ON CONFLICT ON CONSTRAINT uq_family_members_family_user DO UPDATE SET role_code='CHILD', status='ACTIVE', updated_at=NOW();" >/dev/null

ALLOW_ID="$(curl -fsS -X POST "${GATEWAY_URL}/api/v1/allowances/requests" \
  -H "Authorization: Bearer ${OWNER_TOKEN}" -H 'Content-Type: application/json' \
  -d "$(jq -nc --arg f "${FAMILY_ID}" --arg m "${OWNER_MEMBER_ID}" \
    '{familyId:$f,requesterMemberId:$m,amount:1000,reason:"sec-child-approve"}')" | jq -r '.id')"

log "CHILD attempting parent-only approval → 403"
code="$(code_only POST "/api/v1/allowances/requests/${ALLOW_ID}/approve" \
  -H "Authorization: Bearer ${PARENT_TOKEN}" -H 'Content-Type: application/json' \
  -d "$(jq -nc --arg m "${OWNER_MEMBER_ID}" '{approverMemberId:$m,comment:"nope"}')")"
[[ "${code}" == "403" ]] || fail "CHILD approve expected 403 got ${code}"
pass "CHILD approve 403"

# Promote parent to VIEWER and attempt wallet transfer mutation
docker exec ff-postgres psql -U postgres -d family_db -v ON_ERROR_STOP=1 -c \
  "UPDATE family_members SET role_code='VIEWER' WHERE family_id='${FAMILY_ID}' AND user_profile_id='${PARENT_SUB}';" >/dev/null

MEMBER_WALLET="$(curl -fsS -X POST "${GATEWAY_URL}/api/v1/wallets" \
  -H "Authorization: Bearer ${OWNER_TOKEN}" -H 'Content-Type: application/json' \
  -d "$(jq -nc --arg f "${FAMILY_ID}" --arg m "${OWNER_MEMBER_ID}" \
    '{familyId:$f,ownerMemberId:$m,name:"Sec Member",walletType:"MEMBER_WALLET"}')" | jq -r '.id')"

log "VIEWER attempting financial mutation → 403"
code="$(curl -s -o /dev/null -w '%{http_code}' -X POST "${GATEWAY_URL}/api/v1/wallet-transfers" \
  -H "Authorization: Bearer ${PARENT_TOKEN}" -H 'Content-Type: application/json' \
  -d "$(jq -nc --arg s "${WALLET_ID}" --arg d "${MEMBER_WALLET}" --arg r "sec-$RANDOM" --arg k "idem-$RANDOM" \
    '{sourceWalletId:$s,destinationWalletId:$d,amount:1,referenceId:$r,idempotencyKey:$k}')")"
[[ "${code}" == "403" ]] || fail "VIEWER transfer expected 403 got ${code}"
pass "VIEWER mutation 403"

log "All security runtime checks passed"
