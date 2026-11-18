# Keycloak — Family Finance (dev)

## Import realm

Docker Compose (`profile: full`) mounts `infra/keycloak` to `/opt/keycloak/data/import`.

Start with import:

```bash
docker compose --profile full up -d keycloak
# or first boot:
docker compose --profile full run --rm keycloak start-dev --import-realm
```

Realm file: `family-finance-realm.json`

- Realm: `family-finance`
- Issuer: `http://localhost:8180/realms/family-finance`
- Web client (public + PKCE): `family-finance-web`
- Gateway client: `family-finance-gateway` (secret in env — rotate for non-local)
- Services client: `family-finance-services`

Demo users (dev only — change passwords):

| User | Password | Role |
|------|----------|------|
| owner.demo | ChangeMeOwner1! | OWNER |
| parent.demo | ChangeMeParent1! | PARENT |

## Spring Resource Server

```yaml
spring.security.oauth2.resourceserver.jwt.issuer-uri: http://localhost:8180/realms/family-finance
```

Mỗi service phải tự verify JWT — không dựa chỉ vào API Gateway.

## Lưu ý

- Secrets trong realm JSON chỉ dùng local; không dùng production.
- Family-scoped authorization vẫn do Family Service quyết định (role realm ≠ membership trong family).
