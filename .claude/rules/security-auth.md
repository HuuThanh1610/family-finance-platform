---
paths:
  - "**/SecurityConfig.java"
  - "**/JwtDecoderConfig.java"
  - "**/FamilyAuthorizationClient.java"
  - "**/CurrentUserResolver.java"
  - "**/InternalApiKeyFilter.java"
  - "**/security/**/*.java"
  - "**/*Security*Test.java"
---

# Security & authorization

- Keycloak is the IdP. Resource servers validate issuer, signature, and configured audience (`JwtDecoderConfig` + YAML).
- Never trust identity/roles from request bodies or client-supplied `X-User-Id` when `familyfinance.security.enabled=true`.
- Profile mapping: JWT `sub` (UUID) aligns with provisioned profile id; prefer `user_profile_id` claim when present. See profile provisioning.
- Enforce family membership/permissions in the service (`FamilyAuthorizationClient` / family internal authz API). Fail closed if authz is unavailable.
- Gateway JWT is not sufficient alone—downstream services keep independent Resource Server config.
- Keep internal API key auth separate from end-user JWT (`InternalApiKeyFilter`).
- Preserve 401 (unauthenticated) vs 403 (authenticated but forbidden). When changing authz, add negative tests: cross-family, missing membership, CHILD/VIEWER lacking permission.
- Do not disable security in compose/full profile “for convenience.” Local header mode is only for documented `SECURITY_ENABLED=false` dev.
