import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Progress } from '@/components/ui/progress'
import { useAsync } from '@/hooks/useAsync'
import { formatVnd } from '@/lib/utils'
import { budgetApi } from '@/services/family-finance-api'

const schema = z.object({
  name: z.string().min(2),
  allocated: z.number().min(1000),
})

type FormValues = z.infer<typeof schema>

export function BudgetsPage() {
  const { data, loading, error, reload } = useAsync(() => budgetApi.getCurrent(), [])
  const [open, setOpen] = useState(false)
  const [localMsg, setLocalMsg] = useState<string | null>(null)
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', allocated: 1_000_000 },
  })

  if (loading) return <LoadingState />
  if (error || !data) return <ErrorState message={error ?? undefined} onRetry={reload} />

  const overallPct = Math.round((data.totalSpent / data.totalAllocated) * 100)

  return (
    <div>
      <PageHeader
        title="Ngân sách tháng"
        description={`Tháng ${data.month} — theo dõi hạn mức theo danh mục.`}
        actions={<Button onClick={() => setOpen(true)}>Thêm / chỉnh danh mục</Button>}
      />

      <Card className="mb-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm text-muted">Đã chi / Tổng ngân sách</p>
            <p className="mt-1 font-display text-2xl font-extrabold">
              {formatVnd(data.totalSpent)} / {formatVnd(data.totalAllocated)}
            </p>
          </div>
          <Badge variant={overallPct > 100 ? 'danger' : overallPct > 85 ? 'warning' : 'success'}>
            {overallPct}% đã sử dụng
          </Badge>
        </div>
        <Progress className="mt-4" value={data.totalSpent} max={data.totalAllocated} />
      </Card>

      <div className="grid gap-4 md:grid-cols-2">
        {data.categories.map((cat) => {
          const over = cat.spent > cat.allocated
          const pct = Math.round((cat.spent / cat.allocated) * 100)
          return (
            <Card key={cat.id}>
              <div className="mb-3 flex items-start justify-between gap-2">
                <div>
                  <h3 className="font-semibold text-ink">{cat.name}</h3>
                  <p className="text-sm text-muted">
                    {formatVnd(cat.spent)} / {formatVnd(cat.allocated)}
                  </p>
                </div>
                {over ? <Badge variant="danger">Vượt ngân sách</Badge> : <Badge variant="muted">{pct}%</Badge>}
              </div>
              <Progress value={cat.spent} max={cat.allocated} />
            </Card>
          )
        })}
      </div>

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title="Chỉnh ngân sách danh mục"
        description="Thay đổi chỉ áp dụng trên UI mock."
      >
        <form
          className="space-y-4"
          onSubmit={form.handleSubmit((values) => {
            setLocalMsg(
              `Đã lưu (mock) danh mục "${values.name}" với hạn mức ${formatVnd(values.allocated)}.`,
            )
            form.reset()
          })}
        >
          <div>
            <Label htmlFor="name">Tên danh mục</Label>
            <Input id="name" {...form.register('name')} />
          </div>
          <div>
            <Label htmlFor="allocated">Hạn mức (VND)</Label>
            <Input id="allocated" type="number" {...form.register('allocated', { valueAsNumber: true })} />
          </div>
          {localMsg ? <p className="text-sm text-brand-700">{localMsg}</p> : null}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setOpen(false)}>
              Đóng
            </Button>
            <Button type="submit">Lưu</Button>
          </div>
        </form>
      </Dialog>
    </div>
  )
}
