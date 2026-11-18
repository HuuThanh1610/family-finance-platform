import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle } from '@/components/ui/card'
import { useAsync } from '@/hooks/useAsync'
import { formatDateTime } from '@/lib/utils'
import { notificationApi } from '@/services/family-finance-api'
import type { NotificationType } from '@/types/domain'

const types: Array<NotificationType | 'ALL'> = [
  'ALL',
  'ALLOWANCE',
  'BUDGET',
  'PAYMENT',
  'HOUSEHOLD',
  'EXPENSE',
  'SYSTEM',
]

export function NotificationsPage() {
  const { data, loading, error, reload } = useAsync(() => notificationApi.list(), [])
  const [filter, setFilter] = useState<(typeof types)[number]>('ALL')
  const [items, setItems] = useState<typeof data>(null)

  const source = items ?? data
  const filtered = useMemo(() => {
    if (!source) return []
    return source.filter((n) => filter === 'ALL' || n.type === filter)
  }, [source, filter])

  if (loading) return <LoadingState />
  if (error || !data) return <ErrorState message={error ?? undefined} onRetry={reload} />

  return (
    <div>
      <PageHeader
        title="Thông báo"
        description="Cảnh báo ngân sách, phê duyệt và gia dụng."
        actions={
          <Link to="/notifications/preferences">
            <Button variant="secondary">Tuỳ chọn</Button>
          </Link>
        }
      />

      <div className="mb-3 flex flex-wrap gap-1.5">
        {types.map((t) => (
          <Button
            key={t}
            size="sm"
            variant={filter === t ? 'primary' : 'secondary'}
            onClick={() => setFilter(t)}
          >
            {t}
          </Button>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>
            {filtered.filter((n) => !n.read).length} chưa đọc / {filtered.length}
          </CardTitle>
          <Button
            size="sm"
            variant="ghost"
            onClick={() => setItems((source ?? data).map((n) => ({ ...n, read: true })))}
          >
            Đánh dấu đã đọc
          </Button>
        </CardHeader>
        <div className="divide-y divide-border border border-border">
          {filtered.map((n) => (
            <button
              key={n.id}
              type="button"
              className={`flex w-full items-start justify-between gap-3 px-3 py-2.5 text-left ${
                n.read ? 'bg-bg' : 'bg-bg-secondary'
              }`}
              onClick={() =>
                setItems((source ?? data).map((x) => (x.id === n.id ? { ...x, read: true } : x)))
              }
            >
              <div>
                <p className="text-[13px] font-medium text-fg">{n.title}</p>
                <p className="mt-0.5 text-[13px] text-fg-secondary">{n.message}</p>
                <p className="mt-1 text-[11px] text-fg-muted">{formatDateTime(n.createdAt)}</p>
              </div>
              <Badge variant="muted">{n.type}</Badge>
            </button>
          ))}
        </div>
      </Card>
    </div>
  )
}
