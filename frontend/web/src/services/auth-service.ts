import { delay } from '@/lib/utils'
import { currentUser } from '@/mocks/data'
import { USE_MOCK, apiRequest, setAccessToken } from '@/services/api-client'
import type { UserProfile } from '@/types/domain'

const AUTH_KEY = 'ff_auth_session'

export interface AuthSession {
  user: UserProfile
  token: string
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

export const authService = {
  getSession(): AuthSession | null {
    const session = readSession()
    if (session?.token) setAccessToken(session.token)
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
    const session = await apiRequest<AuthSession>('/api/auth/login', {
      method: 'POST',
      body: input,
    })
    writeSession(session)
    return session
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
    const session = await apiRequest<AuthSession>('/api/auth/register', {
      method: 'POST',
      body: input,
    })
    writeSession(session)
    return session
  },

  async requestPasswordReset(email: string): Promise<{ message: string }> {
    if (USE_MOCK) {
      await delay()
      return {
        message: `Đã gửi hướng dẫn đặt lại mật khẩu tới ${email} (mô phỏng — chưa tích hợp email thật).`,
      }
    }
    return apiRequest('/api/auth/forgot-password', { method: 'POST', body: { email } })
  },

  logout() {
    writeSession(null)
  },
}
