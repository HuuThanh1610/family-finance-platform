import { useEffect, useState } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { AuthLayout } from '@/features/auth/LoginPage'
import { useAuth } from '@/features/auth/AuthContext'
import { USE_MOCK } from '@/services/api-client'

export function OidcCallbackPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const { completeOidcLogin } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  const code = params.get('code')
  const state = params.get('state')
  const oauthError = params.get('error_description') ?? params.get('error')

  useEffect(() => {
    if (USE_MOCK) return
    if (oauthError) {
      setError(oauthError)
      return
    }
    if (!code) {
      setError('Thiếu authorization code từ Keycloak')
      return
    }

    let cancelled = false
    void (async () => {
      try {
        // exchangeAuthorizationCode dedupes identical codes (StrictMode / effect re-entry).
        await completeOidcLogin(code, state)
        if (cancelled) return
        setDone(true)
        navigate('/dashboard', { replace: true })
      } catch (err) {
        if (cancelled) return
        setError(err instanceof Error ? err.message : 'Đăng nhập OIDC thất bại')
      }
    })()

    return () => {
      cancelled = true
    }
  }, [code, state, oauthError, completeOidcLogin, navigate])

  if (USE_MOCK) return <Navigate to="/login" replace />
  if (error) {
    return (
      <AuthLayout title="Đăng nhập" subtitle="Không hoàn tất được luồng OIDC.">
        <ErrorState message={error} onRetry={() => navigate('/login', { replace: true })} />
      </AuthLayout>
    )
  }
  if (done) return <Navigate to="/dashboard" replace />
  return (
    <AuthLayout title="Đăng nhập" subtitle="Đang xác thực với Keycloak...">
      <LoadingState label="Đang đổi authorization code..." />
    </AuthLayout>
  )
}
