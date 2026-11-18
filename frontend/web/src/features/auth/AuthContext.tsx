import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { authService, type AuthSession, type LoginInput, type RegisterInput } from '@/services/auth-service'

interface AuthContextValue {
  session: AuthSession | null
  isAuthenticated: boolean
  login: (input: LoginInput) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => authService.getSession())

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isAuthenticated: Boolean(session),
      async login(input) {
        const next = await authService.login(input)
        setSession(next)
      },
      async register(input) {
        const next = await authService.register(input)
        setSession(next)
      },
      logout() {
        authService.logout()
        setSession(null)
      },
    }),
    [session],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
