import { zodResolver } from '@hookform/resolvers/zod'
import type { ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuth } from '@/features/auth/AuthContext'
import { USE_MOCK } from '@/services/api-client'

const schema = z.object({
  email: z.email('Email không hợp lệ'),
  password: z.string().min(6, 'Mật khẩu tối thiểu 6 ký tự'),
})

type FormValues = z.infer<typeof schema>

export function LoginPage() {
  const { login, beginOidcLogin, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: USE_MOCK
      ? { email: 'minh.nguyen@example.com', password: 'demo123' }
      : { email: '', password: '' },
  })

  if (isAuthenticated) return <Navigate to="/dashboard" replace />

  const onSubmit = handleSubmit(async (values) => {
    try {
      await login(values)
      navigate('/dashboard')
    } catch (err) {
      setError('root', {
        message: err instanceof Error ? err.message : 'Đăng nhập thất bại',
      })
    }
  })

  return (
    <AuthLayout
      title="Đăng nhập"
      subtitle={
        USE_MOCK
          ? 'Truy cập nền tảng quản lý tài chính gia đình (mock).'
          : 'Xác thực qua Keycloak (OIDC Authorization Code + PKCE).'
      }
    >
      <Card>
        {USE_MOCK ? (
          <form className="space-y-3" onSubmit={onSubmit}>
            <div>
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" {...register('email')} />
              {errors.email ? <p className="mt-1 text-[12px] text-danger">{errors.email.message}</p> : null}
            </div>
            <div>
              <Label htmlFor="password">Mật khẩu</Label>
              <Input id="password" type="password" {...register('password')} />
              {errors.password ? (
                <p className="mt-1 text-[12px] text-danger">{errors.password.message}</p>
              ) : null}
            </div>
            {errors.root ? <p className="text-[13px] text-danger">{errors.root.message}</p> : null}
            <Button className="w-full" disabled={isSubmitting}>
              {isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
            </Button>
          </form>
        ) : (
          <div className="space-y-3">
            <p className="text-[13px] text-fg-secondary">
              Bạn sẽ được chuyển tới Keycloak để đăng nhập an toàn (OIDC Authorization Code + PKCE).
            </p>
            <Button
              className="w-full"
              onClick={() => {
                void beginOidcLogin()
              }}
            >
              Đăng nhập với Keycloak
            </Button>
          </div>
        )}
        <div className="mt-4 space-y-2 text-[13px] text-fg-secondary">
          <p>
            Chưa có tài khoản?{' '}
            <Link className="font-medium text-fg underline-offset-2 hover:underline" to="/register">
              Đăng ký
            </Link>
          </p>
          <p>
            <Link className="font-medium text-fg underline-offset-2 hover:underline" to="/forgot-password">
              Quên mật khẩu?
            </Link>
          </p>
          {!USE_MOCK ? (
            <p className="border border-border bg-bg-secondary px-2.5 py-2 text-[12px] text-fg-secondary">
              Demo: owner.demo@example.local / ChangeMeOwner1!
            </p>
          ) : (
            <p className="border border-border bg-bg-secondary px-2.5 py-2 text-[12px] text-fg-secondary">
              VITE_USE_MOCK=true — chưa gọi API Gateway.
            </p>
          )}
        </div>
      </Card>
    </AuthLayout>
  )
}

export function AuthLayout({
  title,
  subtitle,
  children,
}: {
  title: string
  subtitle: string
  children: ReactNode
}) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="hidden border-r border-border bg-fg p-10 text-white lg:flex lg:flex-col lg:justify-between">
        <div>
          <p className="text-[15px] font-semibold tracking-tight">Family Finance</p>
          <p className="mt-3 max-w-sm text-[13px] text-white/70">
            Ví chung, ngân sách, tiền tiêu vặt và gia dụng — giao diện làm việc chuyên nghiệp.
          </p>
        </div>
        <ul className="space-y-2 text-[13px] text-white/60">
          <li>Số dư & chi tiêu theo thành viên</li>
          <li>Ngân sách danh mục với cảnh báo vượt hạn</li>
          <li>Kho gia dụng & danh sách mua sắm</li>
        </ul>
      </div>
      <div className="flex items-center justify-center bg-bg-secondary px-4 py-10">
        <div className="w-full max-w-sm">
          <div className="mb-5">
            <p className="mb-2 text-[13px] font-semibold text-fg lg:hidden">Family Finance</p>
            <h1 className="text-[22px] font-semibold text-fg">{title}</h1>
            <p className="mt-1 text-[13px] text-fg-secondary">{subtitle}</p>
          </div>
          {children}
        </div>
      </div>
    </div>
  )
}