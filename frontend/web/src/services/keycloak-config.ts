const issuerBase =
  import.meta.env.VITE_KEYCLOAK_URL?.replace(/\/$/, '') ?? 'http://localhost:8180'

export const KEYCLOAK_REALM = import.meta.env.VITE_KEYCLOAK_REALM ?? 'family-finance'
export const KEYCLOAK_CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'family-finance-web'

export const KEYCLOAK_ISSUER = `${issuerBase}/realms/${KEYCLOAK_REALM}`

export function keycloakAuthorizeUrl(): string {
  return `${KEYCLOAK_ISSUER}/protocol/openid-connect/auth`
}

export function keycloakTokenUrl(): string {
  return `${KEYCLOAK_ISSUER}/protocol/openid-connect/token`
}

export function keycloakLogoutUrl(): string {
  return `${KEYCLOAK_ISSUER}/protocol/openid-connect/logout`
}

export function appRedirectUri(): string {
  const configured = import.meta.env.VITE_OIDC_REDIRECT_URI
  if (configured) return configured
  return `${window.location.origin}/auth/callback`
}

export function appPostLogoutRedirectUri(): string {
  const configured = import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_URI
  if (configured) return configured
  return `${window.location.origin}/login`
}