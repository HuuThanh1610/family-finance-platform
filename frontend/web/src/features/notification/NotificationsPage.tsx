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
        title="Trung tâm thông báo"
        description="Theo dõi cảnh báo ngân sách, phê duyệt và gia dụng."
        actions={
          <Link to="/notifications/preferences">
            <Button variant="secondary">Tuỳ chọn</Button>
          </Link>
        }
      />

      <div className="mb-4 flex flex-wrap gap-2">
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
            {filtered.filter((n) => !n.read).length} chưa đọc / {filtered.length} thông báo
          </CardTitle>
          <Button
            size="sm"
            variant="ghost"
            onClick={() => setItems((source ?? data).map((n) => ({ ...n, read: true })))}
          >
            Đánh dấu đã đọc
          </Button>
        </CardHeader>
        <div className="space-y-3">
          {filtered.map((n) => (
            <button
              key={n.id}
              type="button"
              className={`w-full rounded-xl border px-3 py-3 text-left ${
                n.read ? 'border-line bg-white' : 'border-brand-200 bg-brand-50/50'
              }`}
              onClick={() =>
                setItems((source ?? data).map((x) => (x.id === n.id ? { ...x, read: true } : x)))
              }
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-semibold text-ink">{n.title}</p>
                  <p className="mt-1 text-sm text-muted">{n.message}</p>
                  <p className="mt-1 text-xs text-muted">{formatDateTime(n.createdAt)}</p>
                </div>
                <Badge variant={n.read ? 'muted' : 'default'}>{n.type}</Badge>
              </div>
            </button>
          ))}
        </div>
      </Card>
    </div>
  )
}
