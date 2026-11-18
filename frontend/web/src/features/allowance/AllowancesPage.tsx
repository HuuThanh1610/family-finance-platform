import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
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
      <PageHeader title="Tiền tiêu vặt" description="Chính sách và phê duyệt yêu cầu CHILD." />

      <div className="grid gap-3 lg:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle>Chính sách</CardTitle>
          </CardHeader>
          <div className="space-y-2">
            {policies.data.map((p) => (
              <div key={p.id} className="border border-border p-2.5 text-[13px]">
                <p className="font-medium">{p.childName}</p>
                <p className="text-fg-secondary">
                  {formatVnd(p.monthlyAmount)} / tháng · ngày {p.dayOfMonth}
                </p>
                <Badge className="mt-2" variant={p.active ? 'success' : 'muted'}>
                  {p.active ? 'ACTIVE' : 'PAUSED'}
                </Badge>
              </div>
            ))}
          </div>
        </Card>

        <Card className="lg:col-span-2 !p-0 overflow-hidden">
          <div className="border-b border-border px-4 py-3">
            <CardTitle>Yêu cầu chờ duyệt</CardTitle>
          </div>
          {pending.length === 0 ? (
            <p className="p-4 text-[13px] text-fg-secondary">Không có yêu cầu chờ duyệt.</p>
          ) : (
            <Table>
              <THead>
                <TR>
                  <TH>Người yêu cầu</TH>
                  <TH>Lý do</TH>
                  <TH className="text-right">Số tiền</TH>
                  <TH />
                </TR>
              </THead>
              <TBody>
                {pending.map((req) => (
                  <TR key={req.id}>
                    <TD className="!tabular-nums-none">
                      <p className="font-medium">{req.childName}</p>
                      <p className="text-[12px] text-fg-muted">{formatDateTime(req.requestedAt)}</p>
                    </TD>
                    <TD className="!tabular-nums-none text-fg-secondary">{req.reason}</TD>
                    <TD className="text-right font-medium">{formatVnd(req.amount)}</TD>
                    <TD className="text-right">
                      <div className="flex justify-end gap-1.5">
                        <Button
                          size="sm"
                          onClick={() =>
                            alert('Phê duyệt nối budget-service (learning task). Mock UI only.')
                          }
                        >
                          Duyệt
                        </Button>
                        <Button
                          size="sm"
                          variant="secondary"
                          onClick={() =>
                            alert('Từ chối nối budget-service (learning task). Mock UI only.')
                          }
                        >
                          Từ chối
                        </Button>
                      </div>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          )}
        </Card>
      </div>

      <Card className="mt-3 !p-0 overflow-hidden">
        <div className="border-b border-border px-4 py-3">
          <CardTitle>Lịch sử phê duyệt</CardTitle>
        </div>
        <Table>
          <THead>
            <TR>
              <TH>Yêu cầu</TH>
              <TH>Người duyệt</TH>
              <TH>Trạng thái</TH>
              <TH className="text-right">Số tiền</TH>
            </TR>
          </THead>
          <TBody>
            {history.map((req) => (
              <TR key={req.id}>
                <TD className="!tabular-nums-none">
                  <p className="font-medium">
                    {req.childName} — {req.reason}
                  </p>
                  <p className="text-[12px] text-fg-muted">
                    {req.reviewedAt ? formatDateTime(req.reviewedAt) : '—'}
                    {req.reviewNote ? ` · ${req.reviewNote}` : ''}
                  </p>
                </TD>
                <TD className="text-fg-secondary">{req.reviewerName ?? '—'}</TD>
                <TD>
                  <Badge variant={req.status === 'APPROVED' ? 'success' : 'danger'}>{req.status}</Badge>
                </TD>
                <TD className="text-right font-medium">{formatVnd(req.amount)}</TD>
              </TR>
            ))}
          </TBody>
        </Table>
      </Card>
    </div>
  )
}
