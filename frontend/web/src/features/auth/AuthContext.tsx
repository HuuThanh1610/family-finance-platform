import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
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

  const login = useCallback(async (input: LoginInput) => {
    const next = await authService.login(input)
    setSession(next)
  }, [])

  const register = useCallback(async (input: RegisterInput) => {
    const next = await authService.register(input)
    setSession(next)
  }, [])

  const beginOidcLogin = useCallback(async () => {
    await authService.beginOidcLogin()
  }, [])

  // Stable identity — must not retrigger OidcCallbackPage when session updates.
  const completeOidcLogin = useCallback(async (code: string, state: string | null) => {
    const next = await authService.completeOidcLogin(code, state)
    setSession(next)
  }, [])

  const logout = useCallback(() => {
    authService.logout()
    setSession(null)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isAuthenticated: Boolean(session),
      bootstrapping,
      login,
      register,
      beginOidcLogin,
      completeOidcLogin,
      logout,
    }),
    [session, bootstrapping, login, register, beginOidcLogin, completeOidcLogin, logout],
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