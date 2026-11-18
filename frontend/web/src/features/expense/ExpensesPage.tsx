import { useMemo, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { Paperclip } from 'lucide-react'
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
import { Select } from '@/components/ui/select'
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { useAsync } from '@/hooks/useAsync'
import { formatDate, formatVnd } from '@/lib/utils'
import { demoFamily } from '@/mocks/data'
import { expenseApi } from '@/services/family-finance-api'
import type { Expense } from '@/types/domain'

const schema = z.object({
  title: z.string().min(2),
  amount: z.number().min(1000),
  category: z.string().min(1),
  memberId: z.string().min(1),
  note: z.string().optional(),
})

type FormValues = z.infer<typeof schema>

export function ExpensesPage() {
  const { data, loading, error, reload } = useAsync(() => expenseApi.list(), [])
  const [category, setCategory] = useState('ALL')
  const [memberId, setMemberId] = useState('ALL')
  const [createOpen, setCreateOpen] = useState(false)
  const [detail, setDetail] = useState<Expense | null>(null)
  const [msg, setMsg] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: '',
      amount: 100000,
      category: 'Thực phẩm',
      memberId: 'mem-001',
      note: '',
    },
  })

  const filtered = useMemo(() => {
    if (!data) return []
    return data.filter((e) => {
      const byCat = category === 'ALL' || e.category === category
      const byMem = memberId === 'ALL' || e.memberId === memberId
      return byCat && byMem
    })
  }, [data, category, memberId])

  if (loading) return <LoadingState />
  if (error || !data) return <ErrorState message={error ?? undefined} onRetry={reload} />

  const categories = Array.from(new Set(data.map((e) => e.category)))

  return (
    <div>
      <PageHeader
        title="Chi tiêu"
        description="Danh sách chi tiêu với bộ lọc và đính kèm hóa đơn (UI)."
        actions={<Button onClick={() => setCreateOpen(true)}>Thêm chi tiêu</Button>}
      />

      <Card className="mb-3">
        <div className="grid gap-2 md:grid-cols-3">
          <div>
            <Label>Danh mục</Label>
            <Select value={category} onChange={(e) => setCategory(e.target.value)}>
              <option value="ALL">Tất cả</option>
              {categories.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label>Thành viên</Label>
            <Select value={memberId} onChange={(e) => setMemberId(e.target.value)}>
              <option value="ALL">Tất cả</option>
              {demoFamily.members.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.fullName}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label>Tháng</Label>
            <Input type="month" defaultValue="2026-11" />
          </div>
        </div>
      </Card>

      <Card className="!p-0 overflow-hidden">
        <div className="border-b border-border px-4 py-2.5 text-[12px] text-fg-secondary">
          {filtered.length} khoản chi
        </div>
        <Table>
          <THead>
            <TR>
              <TH>Tiêu đề</TH>
              <TH>Danh mục</TH>
              <TH>Thành viên</TH>
              <TH>Ngày</TH>
              <TH className="text-right">Số tiền</TH>
            </TR>
          </THead>
          <TBody>
            {filtered.map((expense) => (
              <TR key={expense.id} className="cursor-pointer" onClick={() => setDetail(expense)}>
                <TD className="!tabular-nums-none">
                  <span className="font-medium">{expense.title}</span>
                  {expense.hasReceipt ? (
                    <Badge variant="muted" className="ml-2">
                      <Paperclip className="mr-0.5 inline h-3 w-3" strokeWidth={1.75} />
                      Hóa đơn
                    </Badge>
                  ) : null}
                </TD>
                <TD className="text-fg-secondary">{expense.category}</TD>
                <TD className="text-fg-secondary">{expense.memberName}</TD>
                <TD className="text-fg-secondary">{formatDate(expense.occurredAt)}</TD>
                <TD className="text-right font-medium">{formatVnd(expense.amount)}</TD>
              </TR>
            ))}
          </TBody>
        </Table>
      </Card>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} title="Thêm chi tiêu">
        <form
          className="space-y-3"
          onSubmit={form.handleSubmit((values) => {
            setMsg(`Đã tạo mock "${values.title}" — ${formatVnd(values.amount)}.`)
            form.reset()
          })}
        >
          <div>
            <Label>Tiêu đề</Label>
            <Input {...form.register('title')} />
          </div>
          <div>
            <Label>Số tiền</Label>
            <Input type="number" {...form.register('amount', { valueAsNumber: true })} />
          </div>
          <div>
            <Label>Danh mục</Label>
            <Select {...form.register('category')}>
              {categories.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label>Thành viên</Label>
            <Select {...form.register('memberId')}>
              {demoFamily.members.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.fullName}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label>Ghi chú</Label>
            <Textarea {...form.register('note')} />
          </div>
          <div>
            <Label>Đính kèm hóa đơn (UI)</Label>
            <Input type="file" accept="image/*,.pdf" />
            <p className="mt-1 text-[11px] text-fg-muted">Chưa upload storage.</p>
          </div>
          {msg ? <p className="text-[13px] text-fg">{msg}</p> : null}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setCreateOpen(false)}>
              Đóng
            </Button>
            <Button type="submit">Lưu</Button>
          </div>
        </form>
      </Dialog>

      <Dialog open={Boolean(detail)} onClose={() => setDetail(null)} title="Chi tiết chi tiêu">
        {detail ? (
          <dl className="space-y-2 text-[13px]">
            <div className="flex justify-between gap-2">
              <dt className="text-fg-secondary">Mã</dt>
              <dd className="font-medium">{detail.id}</dd>
            </div>
            <div className="flex justify-between gap-2">
              <dt className="text-fg-secondary">Tiêu đề</dt>
              <dd className="font-medium">{detail.title}</dd>
            </div>
            <div className="flex justify-between gap-2">
              <dt className="text-fg-secondary">Số tiền</dt>
              <dd className="font-medium tabular-nums">{formatVnd(detail.amount)}</dd>
            </div>
            <div className="flex justify-between gap-2">
              <dt className="text-fg-secondary">Danh mục</dt>
              <dd className="font-medium">{detail.category}</dd>
            </div>
            <div className="flex justify-between gap-2">
              <dt className="text-fg-secondary">Người chi</dt>
              <dd className="font-medium">{detail.memberName}</dd>
            </div>
            <div className="flex justify-between gap-2">
              <dt className="text-fg-secondary">Ghi chú</dt>
              <dd className="font-medium">{detail.note ?? '—'}</dd>
            </div>
            <div className="flex justify-between gap-2">
              <dt className="text-fg-secondary">Hóa đơn</dt>
              <dd className="font-medium">{detail.hasReceipt ? 'Có (mock)' : 'Không'}</dd>
            </div>
          </dl>
        ) : null}
      </Dialog>
    </div>
  )
}
