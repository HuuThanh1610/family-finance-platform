import { useEffect, useRef, useState } from 'react'
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
  // React StrictMode remounts effects in dev; PKCE verifier is single-use.
  const exchangeStarted = useRef(false)

  useEffect(() => {
    if (USE_MOCK) return
    if (exchangeStarted.current) return
    const code = params.get('code')
    const state = params.get('state')
    const oauthError = params.get('error_description') ?? params.get('error')
    if (oauthError) {
      setError(oauthError)
      return
    }
    if (!code) {
      setError('Thiếu authorization code từ Keycloak')
      return
    }
    exchangeStarted.current = true
    void (async () => {
      try {
        await completeOidcLogin(code, state)
        setDone(true)
        navigate('/dashboard', { replace: true })
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Đăng nhập OIDC thất bại')
      }
    })()
  }, [completeOidcLogin, navigate, params])

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