import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { AuthLayout } from '@/features/auth/LoginPage'
import { useAuth } from '@/features/auth/AuthContext'

const schema = z
  .object({
    fullName: z.string().min(2, 'Họ tên tối thiểu 2 ký tự'),
    email: z.email('Email không hợp lệ'),
    password: z.string().min(6, 'Mật khẩu tối thiểu 6 ký tự'),
    confirmPassword: z.string(),
  })
  .refine((v) => v.password === v.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })

type FormValues = z.infer<typeof schema>

export function RegisterPage() {
  const { register: registerUser, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  if (isAuthenticated) return <Navigate to="/dashboard" replace />

  const onSubmit = handleSubmit(async (values) => {
    try {
      await registerUser(values)
      navigate('/dashboard')
    } catch (err) {
      setError('root', {
        message: err instanceof Error ? err.message : 'Đăng ký thất bại',
      })
    }
  })

  return (
    <AuthLayout title="Tạo tài khoản" subtitle="Bắt đầu quản lý tài chính cho gia đình của bạn.">
      <Card>
        <form className="space-y-4" onSubmit={onSubmit}>
          <div>
            <Label htmlFor="fullName">Họ và tên</Label>
            <Input id="fullName" {...register('fullName')} />
            {errors.fullName ? (
              <p className="mt-1 text-xs text-danger">{errors.fullName.message}</p>
            ) : null}
          </div>
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
          <div>
            <Label htmlFor="confirmPassword">Xác nhận mật khẩu</Label>
            <Input id="confirmPassword" type="password" {...register('confirmPassword')} />
            {errors.confirmPassword ? (
              <p className="mt-1 text-xs text-danger">{errors.confirmPassword.message}</p>
            ) : null}
          </div>
          {errors.root ? <p className="text-sm text-danger">{errors.root.message}</p> : null}
          <Button className="w-full" disabled={isSubmitting}>
            {isSubmitting ? 'Đang tạo...' : 'Đăng ký'}
          </Button>
        </form>
        <p className="mt-4 text-sm text-muted">
          Đã có tài khoản?{' '}
          <Link className="font-semibold text-brand-700" to="/login">
            Đăng nhập
          </Link>
        </p>
      </Card>
    </AuthLayout>
  )
}
