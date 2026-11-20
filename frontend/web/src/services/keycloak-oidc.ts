import { deriveCodeChallenge, generateCodeVerifier, generateOauthState } from '@/lib/pkce'
import { tokenExpiresAtMs } from '@/lib/jwt'
import {
  KEYCLOAK_CLIENT_ID,
  appPostLogoutRedirectUri,
  appRedirectUri,
  keycloakAuthorizeUrl,
  keycloakLogoutUrl,
  keycloakTokenUrl,
} from '@/services/keycloak-config'

const PKCE_KEY = 'ff.oidc.pkce'
const STATE_KEY = 'ff.oidc.state'

export interface OidcTokenResponse {
  access_token: string
  refresh_token?: string
  id_token?: string
  expires_in: number
  token_type: string
}

export interface OidcTokens {
  accessToken: string
  refreshToken?: string
  idToken?: string
  expiresAt: number
}

function storePkce(verifier: string, state: string) {
  sessionStorage.setItem(PKCE_KEY, verifier)
  sessionStorage.setItem(STATE_KEY, state)
}

function consumePkce(): { verifier: string; state: string } | null {
  const verifier = sessionStorage.getItem(PKCE_KEY)
  const state = sessionStorage.getItem(STATE_KEY)
  sessionStorage.removeItem(PKCE_KEY)
  sessionStorage.removeItem(STATE_KEY)
  if (!verifier || !state) return null
  return { verifier, state }
}

export async function beginLoginRedirect(): Promise<void> {
  const verifier = generateCodeVerifier()
  const challenge = await deriveCodeChallenge(verifier)
  const state = generateOauthState()
  storePkce(verifier, state)

  const params = new URLSearchParams({
    client_id: KEYCLOAK_CLIENT_ID,
    redirect_uri: appRedirectUri(),
    response_type: 'code',
    scope: 'openid profile email',
    state,
    code_challenge: challenge,
    code_challenge_method: 'S256',
  })

  window.location.assign(`${keycloakAuthorizeUrl()}?${params.toString()}`)
}

async function requestToken(body: URLSearchParams): Promise<OidcTokens> {
  const response = await fetch(keycloakTokenUrl(), {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })

  if (!response.ok) {
    let detail = 'Không đổi được token OIDC'
    try {
      const payload = (await response.json()) as { error_description?: string; error?: string }
      if (payload.error_description) detail = payload.error_description
      else if (payload.error) detail = payload.error
    } catch {
      /* ignore */
    }
    throw new Error(detail)
  }

  const json = (await response.json()) as OidcTokenResponse
  const jwtExpiry = tokenExpiresAtMs(json.access_token)
  const expiresAt = jwtExpiry ?? Date.now() + json.expires_in * 1000
  return {
    accessToken: json.access_token,
    refreshToken: json.refresh_token,
    idToken: json.id_token,
    expiresAt,
  }
}

export async function exchangeAuthorizationCode(
  code: string,
  state: string | null,
): Promise<OidcTokens> {
  const pkce = consumePkce()
  if (!pkce || (state && pkce.state !== state)) {
    throw new Error('Phiên OIDC không hợp lệ (state/PKCE)')
  }

  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: KEYCLOAK_CLIENT_ID,
    code,
    redirect_uri: appRedirectUri(),
    code_verifier: pkce.verifier,
  })

  return requestToken(body)
}

export async function refreshTokens(refreshToken: string): Promise<OidcTokens> {
  const body = new URLSearchParams({
    grant_type: 'refresh_token',
    client_id: KEYCLOAK_CLIENT_ID,
    refresh_token: refreshToken,
  })
  return requestToken(body)
}

export function buildLogoutRedirect(idToken?: string): string {
  const params = new URLSearchParams({
    client_id: KEYCLOAK_CLIENT_ID,
    post_logout_redirect_uri: appPostLogoutRedirectUri(),
  })
  if (idToken) {
    params.set('id_token_hint', idToken)
  }
  return `${keycloakLogoutUrl()}?${params.toString()}`
}

export function tokensNeedRefresh(expiresAt: number | undefined, skewMs = 60_000): boolean {
  if (!expiresAt) return false
  return Date.now() >= expiresAt - skewMs
}