import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
import { useAsync } from '@/hooks/useAsync'
import { formatDateTime, formatVnd } from '@/lib/utils'
import { paymentApi } from '@/services/family-finance-api'
import type { PaymentProviderCode, PaymentStatus } from '@/types/domain'

const amounts = [100_000, 200_000, 500_000, 1_000_000, 2_000_000]

const schema = z.object({
  amount: z.number().min(10_000, 'Tối thiểu 10.000₫'),
  provider: z.enum(['MOMO_SANDBOX', 'DEMO_PROVIDER']),
  description: z.string().min(3, 'Nhập mô tả'),
})

type FormValues = z.infer<typeof schema>

const statusVariant: Record<PaymentStatus, 'warning' | 'success' | 'danger' | 'muted' | 'info'> = {
  PENDING: 'warning',
  PROCESSING: 'info',
  SUCCESS: 'success',
  FAILED: 'danger',
  CANCELLED: 'muted',
}

export function PaymentsPage() {
  const { data, loading, error, reload } = useAsync(() => paymentApi.listOrders(), [])
  const [preset, setPreset] = useState<number | null>(500_000)
  const [resultMsg, setResultMsg] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      amount: 500_000,
      provider: 'MOMO_SANDBOX',
      description: 'Nạp ví chung (demo)',
    },
  })

  if (loading) return <LoadingState />
  if (error || !data) return <ErrorState message={error ?? undefined} onRetry={reload} />

  return (
    <div>
      <PageHeader
        title="Nạp tiền"
        description="Top-up qua PaymentProvider abstraction. Không thực hiện thanh toán thật."
      />

      <div className="mb-3 border border-border bg-bg px-3 py-2 text-[13px] text-fg-secondary">
        Mọi đơn trên trang này đều là mô phỏng. MoMo Sandbox chưa kết nối — không có credential.
      </div>

      <div className="grid gap-3 lg:grid-cols-5">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Tạo đơn nạp</CardTitle>
          </CardHeader>
          <form
            className="space-y-3"
            onSubmit={form.handleSubmit(async (values) => {
              const order = await paymentApi.createTopUp({
                amount: values.amount,
                provider: values.provider as PaymentProviderCode,
                description: values.description,
              })
              setResultMsg(order.note ?? `Đã tạo đơn ${order.id}`)
              reload()
            })}
          >
            <div>
              <Label>Số tiền nhanh</Label>
              <div className="mt-1.5 flex flex-wrap gap-1.5">
                {amounts.map((amount) => (
                  <Button
                    key={amount}
                    type="button"
                    size="sm"
                    variant={preset === amount ? 'primary' : 'secondary'}
                    onClick={() => {
                      setPreset(amount)
                      form.setValue('amount', amount)
                    }}
                  >
                    {formatVnd(amount)}
                  </Button>
                ))}
              </div>
            </div>
            <div>
              <Label htmlFor="amount">Số tiền (VND)</Label>
              <Input
                id="amount"
                type="number"
                {...form.register('amount', {
                  valueAsNumber: true,
                  onChange: () => setPreset(null),
                })}
              />
              {form.formState.errors.amount ? (
                <p className="mt-1 text-[12px] text-danger">{form.formState.errors.amount.message}</p>
              ) : null}
            </div>
            <div>
              <Label htmlFor="provider">Nhà cung cấp</Label>
              <Select id="provider" {...form.register('provider')}>
                <option value="MOMO_SANDBOX">MoMo Sandbox (chưa kết nối)</option>
                <option value="DEMO_PROVIDER">Demo Provider</option>
              </Select>
            </div>
            <div>
              <Label htmlFor="description">Mô tả</Label>
              <Input id="description" {...form.register('description')} />
            </div>
            {resultMsg ? (
              <p className="border border-border bg-bg-secondary px-2.5 py-2 text-[13px]">{resultMsg}</p>
            ) : null}
            <Button className="w-full" disabled={form.formState.isSubmitting}>
              {form.formState.isSubmitting ? 'Đang tạo...' : 'Tạo đơn mô phỏng'}
            </Button>
          </form>
        </Card>

        <Card className="lg:col-span-3 !p-0 overflow-hidden">
          <div className="border-b border-border px-4 py-3">
            <CardTitle>Lịch sử đơn</CardTitle>
          </div>
          <Table>
            <THead>
              <TR>
                <TH>Mô tả</TH>
                <TH>Provider</TH>
                <TH>Trạng thái</TH>
                <TH className="text-right">Số tiền</TH>
              </TR>
            </THead>
            <TBody>
              {data.map((order) => (
                <TR key={order.id}>
                  <TD className="!tabular-nums-none">
                    <p className="font-medium">{order.description}</p>
                    <p className="text-[12px] text-fg-muted">
                      {order.id} · {formatDateTime(order.createdAt)}
                    </p>
                  </TD>
                  <TD className="text-fg-secondary">{order.provider}</TD>
                  <TD>
                    <Badge variant={statusVariant[order.status]}>{order.status}</Badge>
                  </TD>
                  <TD className="text-right font-medium">{formatVnd(order.amount)}</TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </Card>
      </div>
    </div>
  )
}
