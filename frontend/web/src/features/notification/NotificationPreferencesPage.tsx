import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Button } from '@/components/ui/button'
import { Card, CardTitle } from '@/components/ui/card'
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
import { useAsync } from '@/hooks/useAsync'
import { notificationApi } from '@/services/family-finance-api'
import type { NotificationPreference } from '@/types/domain'

export function NotificationPreferencesPage() {
  const { data, loading, error, reload } = useAsync(() => notificationApi.preferences(), [])
  const [prefs, setPrefs] = useState<NotificationPreference[] | null>(null)
  const [saved, setSaved] = useState(false)

  const rows = prefs ?? data

  if (loading) return <LoadingState />
  if (error || !data) return <ErrorState message={error ?? undefined} onRetry={reload} />

  return (
    <div>
      <PageHeader
        title="Tuỳ chọn thông báo"
        description="Bật/tắt kênh nhận theo loại sự kiện."
        actions={
          <Link to="/notifications">
            <Button variant="secondary">Quay lại</Button>
          </Link>
        }
      />

      <Card className="!p-0 overflow-hidden">
        <div className="flex items-center justify-between border-b border-border px-4 py-3">
          <CardTitle>Kênh nhận</CardTitle>
          <Button size="sm" onClick={() => setSaved(true)}>
            Lưu (mock)
          </Button>
        </div>
        <Table>
          <THead>
            <TR>
              <TH>Loại</TH>
              <TH>Email</TH>
              <TH>In-app</TH>
              <TH>Push</TH>
            </TR>
          </THead>
          <TBody>
            {(rows ?? []).map((row) => (
              <TR key={row.type}>
                <TD className="!tabular-nums-none font-medium">{row.type}</TD>
                {(['email', 'inApp', 'push'] as const).map((channel) => (
                  <TD key={channel}>
                    <input
                      type="checkbox"
                      checked={row[channel]}
                      onChange={(e) => {
                        setSaved(false)
                        setPrefs(
                          (rows ?? []).map((r) =>
                            r.type === row.type ? { ...r, [channel]: e.target.checked } : r,
                          ),
                        )
                      }}
                    />
                  </TD>
                ))}
              </TR>
            ))}
          </TBody>
        </Table>
        {saved ? (
          <p className="border-t border-border px-4 py-2 text-[13px] text-fg-secondary">
            Đã lưu tuỳ chọn trên UI mock.
          </p>
        ) : null}
      </Card>
    </div>
  )
}
