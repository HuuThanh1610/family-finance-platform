import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { AuthLayout } from '@/features/auth/LoginPage'
import { authService } from '@/services/auth-service'

const schema = z.object({
  email: z.email('Email không hợp lệ'),
})

type FormValues = z.infer<typeof schema>

export function ForgotPasswordPage() {
  const [message, setMessage] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  const onSubmit = handleSubmit(async (values) => {
    const result = await authService.requestPasswordReset(values.email)
    setMessage(result.message)
  })

  return (
    <AuthLayout
      title="Quên mật khẩu"
      subtitle="Nhập email để nhận hướng dẫn đặt lại mật khẩu (mô phỏng)."
    >
      <Card>
        <form className="space-y-4" onSubmit={onSubmit}>
          <div>
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" {...register('email')} />
            {errors.email ? <p className="mt-1 text-xs text-danger">{errors.email.message}</p> : null}
          </div>
          {message ? (
            <p className="rounded-lg bg-brand-50 px-3 py-2 text-sm text-brand-800">{message}</p>
          ) : null}
          <Button className="w-full" disabled={isSubmitting}>
            {isSubmitting ? 'Đang gửi...' : 'Gửi hướng dẫn'}
          </Button>
        </form>
        <p className="mt-4 text-sm text-muted">
          <Link className="font-semibold text-brand-700" to="/login">
            Quay lại đăng nhập
          </Link>
        </p>
      </Card>
    </AuthLayout>
  )
}
