import { delay } from '@/lib/utils'
import { parseJwtPayload } from '@/lib/jwt'
import { currentUser } from '@/mocks/data'
import { ApiError, USE_MOCK, apiRequest, setAccessToken } from '@/services/api-client'
import {
  beginLoginRedirect,
  buildLogoutRedirect,
  exchangeAuthorizationCode,
  refreshTokens,
  tokensNeedRefresh,
  type OidcTokens,
} from '@/services/keycloak-oidc'
import type { UserProfile } from '@/types/domain'

const AUTH_KEY = 'ff_auth_session'

export interface AuthSession {
  user: UserProfile
  token: string
  refreshToken?: string
  idToken?: string
  expiresAt?: number
  mock: boolean
}

export interface LoginInput {
  email: string
  password: string
}

export interface RegisterInput {
  fullName: string
  email: string
  password: string
}

type BackendProfile = {
  id: string
  keycloakSub?: string
  email: string
  displayName: string
  phone?: string | null
  avatarUrl?: string | null
}

function mapProfile(p: BackendProfile): UserProfile {
  return {
    id: p.id,
    email: p.email,
    fullName: p.displayName,
    phone: p.phone ?? undefined,
    avatarUrl: p.avatarUrl ?? undefined,
  }
}

function readSession(): AuthSession | null {
  const raw = localStorage.getItem(AUTH_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthSession
  } catch {
    return null
  }
}

function writeSession(session: AuthSession | null) {
  if (!session) {
    localStorage.removeItem(AUTH_KEY)
    setAccessToken(null)
    return
  }
  localStorage.setItem(AUTH_KEY, JSON.stringify(session))
  setAccessToken(session.token)
}

function sessionFromOidc(tokens: OidcTokens, user: UserProfile): AuthSession {
  return {
    user,
    token: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    idToken: tokens.idToken,
    expiresAt: tokens.expiresAt,
    mock: false,
  }
}

async function provisionIfNeeded(tokens: OidcTokens): Promise<UserProfile> {
  setAccessToken(tokens.accessToken)
  try {
    const me = await apiRequest<BackendProfile>('/api/v1/profiles/me', { skipAuthRetry: true })
    return mapProfile(me)
  } catch (err) {
    if (!(err instanceof ApiError) || (err.status !== 404 && err.status !== 401)) {
      throw err
    }
    const claims = parseJwtPayload(tokens.accessToken)
    const body = {
      keycloakSub: claims.sub ?? '',
      email: claims.email ?? `${claims.preferred_username ?? 'user'}@unknown.local`,
      displayName: claims.name ?? claims.preferred_username ?? 'Người dùng',
    }
    if (!body.keycloakSub) {
      throw err instanceof Error ? err : new Error('Không đọc được JWT sub')
    }
    const created = await apiRequest<BackendProfile>('/api/v1/profiles/provision', {
      method: 'POST',
      body,
      skipAuthRetry: true,
    })
    return mapProfile(created)
  }
}

let refreshInFlight: Promise<boolean> | null = null

export const authService = {
  getSession(): AuthSession | null {
    const session = readSession()
    if (session?.token) setAccessToken(session.token)
    return session
  },

  async ensureAccessToken(): Promise<string | null> {
    const session = readSession()
    if (!session?.token) return null
    if (session.mock || !session.refreshToken) return session.token
    if (!tokensNeedRefresh(session.expiresAt)) return session.token

    const ok = await this.tryRefreshSession()
    if (!ok) return session.token
    return readSession()?.token ?? null
  },

  async tryRefreshSession(): Promise<boolean> {
    if (refreshInFlight) return refreshInFlight
    const session = readSession()
    if (!session?.refreshToken || session.mock) return false

    refreshInFlight = (async () => {
      try {
        const tokens = await refreshTokens(session.refreshToken!)
        const next = sessionFromOidc(tokens, session.user)
        writeSession(next)
        return true
      } catch {
        writeSession(null)
        return false
      } finally {
        refreshInFlight = null
      }
    })()

    return refreshInFlight
  },

  async restoreSession(): Promise<AuthSession | null> {
    const session = readSession()
    if (!session) return null
    if (session.mock) {
      setAccessToken(session.token)
      return session
    }
    if (session.refreshToken && tokensNeedRefresh(session.expiresAt)) {
      const ok = await this.tryRefreshSession()
      if (!ok) return null
      return readSession()
    }
    setAccessToken(session.token)
    return session
  },

  beginOidcLogin() {
    return beginLoginRedirect()
  },

  async completeOidcLogin(code: string, state: string | null): Promise<AuthSession> {
    const tokens = await exchangeAuthorizationCode(code, state)
    const user = await provisionIfNeeded(tokens)
    const session = sessionFromOidc(tokens, user)
    writeSession(session)
    return session
  },

  async login(input: LoginInput): Promise<AuthSession> {
    if (USE_MOCK) {
      await delay()
      if (!input.email || !input.password) {
        throw new Error('Email và mật khẩu là bắt buộc')
      }
      const session: AuthSession = {
        user: { ...currentUser, email: input.email },
        token: 'mock-jwt-token',
        mock: true,
      }
      writeSession(session)
      return session
    }
    await this.beginOidcLogin()
    throw new Error('Chuyển hướng Keycloak')
  },

  async register(input: RegisterInput): Promise<AuthSession> {
    if (USE_MOCK) {
      await delay()
      const session: AuthSession = {
        user: {
          id: 'usr-new',
          email: input.email,
          fullName: input.fullName,
        },
        token: 'mock-jwt-token',
        mock: true,
      }
      writeSession(session)
      return session
    }
    throw new Error('Đăng ký bị tắt — dùng tài khoản Keycloak do quản trị cấp.')
  },

  async requestPasswordReset(email: string): Promise<{ message: string }> {
    if (USE_MOCK) {
      await delay()
      return {
        message: `Đã gửi hướng dẫn đặt lại mật khẩu tới ${email} (mô phỏng — chưa tích hợp email thật).`,
      }
    }
    const base = import.meta.env.VITE_KEYCLOAK_URL?.replace(/\/$/, '') ?? 'http://localhost:8180'
    const realm = import.meta.env.VITE_KEYCLOAK_REALM ?? 'family-finance'
    return {
      message: `Đặt lại mật khẩu qua Keycloak: ${base}/realms/${realm}/login-actions/reset-credentials`,
    }
  },

  logout(options?: { endOidcSession?: boolean }) {
    const session = readSession()
    writeSession(null)
    if (!USE_MOCK && options?.endOidcSession !== false && session?.idToken) {
      window.location.assign(buildLogoutRedirect(session.idToken))
    }
  },
}