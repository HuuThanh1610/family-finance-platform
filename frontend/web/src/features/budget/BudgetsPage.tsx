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
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
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
        title="Ngân sách"
        description={`Tháng ${data.month} — hạn mức theo danh mục.`}
        actions={<Button onClick={() => setOpen(true)}>Chỉnh danh mục</Button>}
      />

      <Card className="mb-3">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-[12px] text-fg-secondary">Đã chi / Tổng ngân sách</p>
            <p className="mt-1 text-[18px] font-semibold tabular-nums">
              {formatVnd(data.totalSpent)} / {formatVnd(data.totalAllocated)}
            </p>
          </div>
          <Badge variant={overallPct > 100 ? 'danger' : overallPct > 85 ? 'warning' : 'muted'}>
            {overallPct}%
          </Badge>
        </div>
        <Progress className="mt-3" value={data.totalSpent} max={data.totalAllocated} />
      </Card>

      <Card className="!p-0 overflow-hidden">
        <Table>
          <THead>
            <TR>
              <TH>Danh mục</TH>
              <TH className="text-right">Đã chi</TH>
              <TH className="text-right">Hạn mức</TH>
              <TH className="w-40">Tiến độ</TH>
              <TH>Trạng thái</TH>
            </TR>
          </THead>
          <TBody>
            {data.categories.map((cat) => {
              const over = cat.spent > cat.allocated
              const pct = Math.round((cat.spent / cat.allocated) * 100)
              return (
                <TR key={cat.id}>
                  <TD className="!tabular-nums-none font-medium">{cat.name}</TD>
                  <TD className="text-right">{formatVnd(cat.spent)}</TD>
                  <TD className="text-right text-fg-secondary">{formatVnd(cat.allocated)}</TD>
                  <TD>
                    <Progress value={cat.spent} max={cat.allocated} />
                  </TD>
                  <TD>
                    {over ? <Badge variant="danger">Vượt</Badge> : <Badge variant="muted">{pct}%</Badge>}
                  </TD>
                </TR>
              )
            })}
          </TBody>
        </Table>
      </Card>

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title="Chỉnh ngân sách danh mục"
        description="Thay đổi chỉ áp dụng trên UI mock."
      >
        <form
          className="space-y-3"
          onSubmit={form.handleSubmit((values) => {
            setLocalMsg(`Đã lưu (mock) "${values.name}" · ${formatVnd(values.allocated)}.`)
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
          {localMsg ? <p className="text-[13px] text-fg">{localMsg}</p> : null}
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
