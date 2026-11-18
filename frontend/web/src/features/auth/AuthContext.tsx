import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { LoadingState } from '@/components/common/LoadingState'
import {
  authService,
  type AuthSession,
  type LoginInput,
  type RegisterInput,
} from '@/services/auth-service'
import { USE_MOCK, configureApiAuth, setUnauthorizedHandler } from '@/services/api-client'

interface AuthContextValue {
  session: AuthSession | null
  isAuthenticated: boolean
  bootstrapping: boolean
  login: (input: LoginInput) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  beginOidcLogin: () => Promise<void>
  completeOidcLogin: (code: string, state: string | null) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [session, setSession] = useState<AuthSession | null>(() => authService.getSession())
  const [bootstrapping, setBootstrapping] = useState(!USE_MOCK)

  useEffect(() => {
    configureApiAuth({
      ensureAccessToken: () => authService.ensureAccessToken(),
      tryRefreshSession: () => authService.tryRefreshSession(),
    })
    setUnauthorizedHandler(() => {
      setSession(null)
      navigate('/login', { replace: true })
    })
  }, [navigate])

  useEffect(() => {
    if (USE_MOCK) {
      setBootstrapping(false)
      return
    }
    let cancelled = false
    void authService.restoreSession().then((next) => {
      if (!cancelled) {
        setSession(next)
        setBootstrapping(false)
      }
    })
    return () => {
      cancelled = true
    }
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isAuthenticated: Boolean(session),
      bootstrapping,
      async login(input) {
        const next = await authService.login(input)
        setSession(next)
      },
      async register(input) {
        const next = await authService.register(input)
        setSession(next)
      },
      async beginOidcLogin() {
        await authService.beginOidcLogin()
      },
      async completeOidcLogin(code, state) {
        const next = await authService.completeOidcLogin(code, state)
        setSession(next)
      },
      logout() {
        authService.logout()
        setSession(null)
      },
    }),
    [session, bootstrapping],
  )

  if (bootstrapping) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-bg-secondary p-6">
        <LoadingState label="Đang khôi phục phiên..." />
      </div>
    )
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}