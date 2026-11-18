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

const schema = z.object({
  email: z.email('Email không hợp lệ'),
  password: z.string().min(6, 'Mật khẩu tối thiểu 6 ký tự'),
})

type FormValues = z.infer<typeof schema>

export function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: 'minh.nguyen@example.com', password: 'demo123' },
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
      subtitle="Quản lý tài chính gia đình một cách rõ ràng và minh bạch."
    >
      <Card>
        <form className="space-y-4" onSubmit={onSubmit}>
          <div>
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" {...register('email')} />
            {errors.email ? <p className="mt-1 text-xs text-danger">{errors.email.message}</p> : null}
          </div>
          <div>
            <Label htmlFor="password">Mật khẩu</Label>
            <Input id="password" type="password" {...register('password')} />
            {errors.password ? (
              <p className="mt-1 text-xs text-danger">{errors.password.message}</p>
            ) : null}
          </div>
          {errors.root ? <p className="text-sm text-danger">{errors.root.message}</p> : null}
          <Button className="w-full" disabled={isSubmitting}>
            {isSubmitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </Button>
        </form>
        <div className="mt-4 space-y-2 text-sm text-muted">
          <p>
            Chưa có tài khoản?{' '}
            <Link className="font-semibold text-brand-700" to="/register">
              Đăng ký
            </Link>
          </p>
          <p>
            <Link className="font-semibold text-brand-700" to="/forgot-password">
              Quên mật khẩu?
            </Link>
          </p>
          <p className="rounded-lg bg-amber-50 px-3 py-2 text-xs text-amber-800">
            Xác thực hiện tại dùng mock adapter — chưa tích hợp Keycloak/OAuth2.
          </p>
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
      <div className="relative hidden overflow-hidden bg-brand-800 p-10 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(174,225,212,0.25),transparent_40%)]" />
        <div className="relative">
          <p className="font-display text-2xl font-extrabold">Family Finance</p>
          <p className="mt-2 max-w-md text-brand-100">
            Nền tảng quản lý ví chung, ngân sách, tiền tiêu vặt và gia dụng cho hộ gia đình Việt Nam.
          </p>
        </div>
        <div className="relative space-y-3 text-sm text-brand-50">
          <p>• Tổng hợp số dư & chi tiêu theo thành viên</p>
          <p>• Ngân sách theo danh mục với cảnh báo vượt hạn mức</p>
          <p>• Kho gia dụng & danh sách mua sắm</p>
        </div>
      </div>
      <div className="flex items-center justify-center px-4 py-10">
        <div className="w-full max-w-md">
          <div className="mb-6">
            <p className="mb-2 font-display text-sm font-bold text-brand-700 lg:hidden">
              Family Finance
            </p>
            <h1 className="text-3xl font-extrabold text-ink">{title}</h1>
            <p className="mt-2 text-sm text-muted">{subtitle}</p>
          </div>
          {children}
        </div>
      </div>
    </div>
  )
}
