# Keycloak (placeholder)

Keycloak được đưa vào Docker Compose với profile `full` để chuẩn bị OAuth2/OIDC.

## Việc owner cần triển khai

1. Tạo realm `family-finance`.
2. Tạo client cho web (public) và từng resource server (confidential hoặc bearer-only).
3. Map roles: `OWNER`, `PARENT`, `ADULT_MEMBER`, `CHILD`, `VIEWER`.
4. Cấu hình Spring Security OAuth2 Resource Server JWT verification trên mỗi service.
5. Enforce resource-level authorization theo `familyId` / membership — **không tin** `familyId` từ frontend.

Chưa có realm export production trong repo này.
