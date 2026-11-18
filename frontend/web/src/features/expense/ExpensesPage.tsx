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
import { Card, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
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
        description="Danh sách chi tiêu gia đình với bộ lọc và đính kèm hóa đơn (UI)."
        actions={<Button onClick={() => setCreateOpen(true)}>Thêm chi tiêu</Button>}
      />

      <Card className="mb-4">
        <div className="grid gap-3 md:grid-cols-3">
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
            <Label>Ngày (demo filter)</Label>
            <Input type="month" defaultValue="2026-11" />
          </div>
        </div>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{filtered.length} khoản chi</CardTitle>
        </CardHeader>
        <div className="space-y-3">
          {filtered.map((expense) => (
            <button
              key={expense.id}
              type="button"
              onClick={() => setDetail(expense)}
              className="flex w-full items-center justify-between gap-3 rounded-xl border border-line px-3 py-3 text-left hover:bg-brand-50/40"
            >
              <div>
                <p className="font-semibold">{expense.title}</p>
                <p className="text-xs text-muted">
                  {expense.memberName} · {formatDate(expense.occurredAt)} · {expense.category}
                </p>
              </div>
              <div className="text-right">
                <p className="font-semibold">{formatVnd(expense.amount)}</p>
                {expense.hasReceipt ? (
                  <Badge variant="info" className="mt-1">
                    <Paperclip className="mr-1 inline h-3 w-3" />
                    Hóa đơn
                  </Badge>
                ) : null}
              </div>
            </button>
          ))}
        </div>
      </Card>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} title="Thêm chi tiêu">
        <form
          className="space-y-3"
          onSubmit={form.handleSubmit((values) => {
            setMsg(`Đã tạo chi tiêu mock "${values.title}" — ${formatVnd(values.amount)}.`)
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
            <p className="mt-1 text-xs text-muted">Chưa upload lên storage — chỉ giao diện.</p>
          </div>
          {msg ? <p className="text-sm text-brand-700">{msg}</p> : null}
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
          <div className="space-y-2 text-sm">
            <p>
              <span className="text-muted">Mã:</span> {detail.id}
            </p>
            <p>
              <span className="text-muted">Tiêu đề:</span> {detail.title}
            </p>
            <p>
              <span className="text-muted">Số tiền:</span> {formatVnd(detail.amount)}
            </p>
            <p>
              <span className="text-muted">Danh mục:</span> {detail.category}
            </p>
            <p>
              <span className="text-muted">Người chi:</span> {detail.memberName}
            </p>
            <p>
              <span className="text-muted">Ghi chú:</span> {detail.note ?? '—'}
            </p>
            <p>
              <span className="text-muted">Hóa đơn:</span>{' '}
              {detail.hasReceipt ? 'Có (mock attachment)' : 'Không'}
            </p>
          </div>
        ) : null}
      </Dialog>
    </div>
  )
}
