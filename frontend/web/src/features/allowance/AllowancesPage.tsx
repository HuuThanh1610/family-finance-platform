import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle } from '@/components/ui/card'
import { useAsync } from '@/hooks/useAsync'
import { formatDateTime, formatVnd } from '@/lib/utils'
import { allowanceApi } from '@/services/family-finance-api'

export function AllowancesPage() {
  const policies = useAsync(() => allowanceApi.listPolicies(), [])
  const requests = useAsync(() => allowanceApi.listRequests(), [])

  if (policies.loading || requests.loading) return <LoadingState />
  if (policies.error || requests.error || !policies.data || !requests.data) {
    return (
      <ErrorState
        message={policies.error ?? requests.error ?? undefined}
        onRetry={() => {
          policies.reload()
          requests.reload()
        }}
      />
    )
  }

  const pending = requests.data.filter((r) => r.status === 'PENDING')
  const history = requests.data.filter((r) => r.status !== 'PENDING')

  return (
    <div>
      <PageHeader
        title="Tiền tiêu vặt"
        description="Chính sách và phê duyệt yêu cầu của thành viên CHILD."
      />

      <div className="grid gap-4 lg:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle>Chính sách</CardTitle>
          </CardHeader>
          <div className="space-y-3">
            {policies.data.map((p) => (
              <div key={p.id} className="rounded-xl border border-line p-3 text-sm">
                <p className="font-semibold">{p.childName}</p>
                <p className="text-muted">
                  {formatVnd(p.monthlyAmount)} / tháng · ngày {p.dayOfMonth}
                </p>
                <Badge className="mt-2" variant={p.active ? 'success' : 'muted'}>
                  {p.active ? 'Đang áp dụng' : 'Tạm dừng'}
                </Badge>
              </div>
            ))}
          </div>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Yêu cầu chờ duyệt</CardTitle>
          </CardHeader>
          <div className="space-y-3">
            {pending.length === 0 ? (
              <p className="text-sm text-muted">Không có yêu cầu chờ duyệt.</p>
            ) : (
              pending.map((req) => (
                <div
                  key={req.id}
                  className="flex flex-col gap-3 rounded-xl border border-line p-3 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div>
                    <p className="font-semibold">
                      {req.childName} · {formatVnd(req.amount)}
                    </p>
                    <p className="text-sm text-muted">{req.reason}</p>
                    <p className="text-xs text-muted">{formatDateTime(req.requestedAt)}</p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      onClick={() =>
                        alert('Phê duyệt sẽ được nối với budget-service (learning task). Mock UI only.')
                      }
                    >
                      Duyệt
                    </Button>
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() =>
                        alert('Từ chối sẽ được nối với budget-service (learning task). Mock UI only.')
                      }
                    >
                      Từ chối
                    </Button>
                  </div>
                </div>
              ))
            )}
          </div>
        </Card>
      </div>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle>Lịch sử phê duyệt</CardTitle>
        </CardHeader>
        <div className="space-y-3">
          {history.map((req) => (
            <div
              key={req.id}
              className="flex flex-col gap-2 rounded-xl border border-line px-3 py-3 sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <p className="font-medium">
                  {req.childName} — {req.reason}
                </p>
                <p className="text-xs text-muted">
                  {req.reviewerName} · {req.reviewedAt ? formatDateTime(req.reviewedAt) : '—'}
                  {req.reviewNote ? ` · ${req.reviewNote}` : ''}
                </p>
              </div>
              <div className="text-right">
                <p className="font-semibold">{formatVnd(req.amount)}</p>
                <Badge variant={req.status === 'APPROVED' ? 'success' : 'danger'}>{req.status}</Badge>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  )
}
