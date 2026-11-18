import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle } from '@/components/ui/card'
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
        description="Bật/tắt kênh nhận thông báo theo loại sự kiện."
        actions={
          <Link to="/notifications">
            <Button variant="secondary">Quay lại</Button>
          </Link>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>Kênh nhận</CardTitle>
          <Button
            size="sm"
            onClick={() => {
              setSaved(true)
            }}
          >
            Lưu (mock)
          </Button>
        </CardHeader>
        <div className="overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="border-b border-line text-muted">
              <tr>
                <th className="px-2 py-2">Loại</th>
                <th className="px-2 py-2">Email</th>
                <th className="px-2 py-2">In-app</th>
                <th className="px-2 py-2">Push</th>
              </tr>
            </thead>
            <tbody>
              {(rows ?? []).map((row) => (
                <tr key={row.type} className="border-b border-line/70">
                  <td className="px-2 py-3 font-medium">{row.type}</td>
                  {(['email', 'inApp', 'push'] as const).map((channel) => (
                    <td key={channel} className="px-2 py-3">
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
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {saved ? (
          <p className="mt-3 text-sm text-brand-700">Đã lưu tuỳ chọn trên UI mock.</p>
        ) : null}
      </Card>
    </div>
  )
}
