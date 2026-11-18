import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Button } from '@/components/ui/button'
import { Card, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Progress } from '@/components/ui/progress'
import { useAsync } from '@/hooks/useAsync'
import { formatVnd } from '@/lib/utils'
import { analyticsApi } from '@/services/family-finance-api'

export function AnalyticsPage() {
  const { data, loading, error, reload } = useAsync(() => analyticsApi.getReports(), [])

  if (loading) return <LoadingState />
  if (error || !data) return <ErrorState message={error ?? undefined} onRetry={reload} />

  return (
    <div>
      <PageHeader
        title="Phân tích tài chính"
        description="Xu hướng chi tiêu, theo thành viên/danh mục và ngân sách thực tế."
        actions={
          <Button
            variant="secondary"
            onClick={() =>
              alert('Export báo cáo sẽ nối analytics-service. UI mock — chưa tạo file thật.')
            }
          >
            Xuất báo cáo
          </Button>
        }
      />

      <div className="grid gap-4 xl:grid-cols-2">
        <Card>
          <CardHeader>
            <div>
              <CardTitle>Xu hướng thu / chi</CardTitle>
              <CardDescription>6 tháng gần nhất</CardDescription>
            </div>
          </CardHeader>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={data.monthlyTrends}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e3e9ef" />
                <XAxis dataKey="month" />
                <YAxis tickFormatter={(v) => `${Math.round(Number(v) / 1_000_000)}tr`} width={40} />
                <Tooltip formatter={(v) => formatVnd(Number(v))} />
                <Legend />
                <Line type="monotone" dataKey="income" name="Thu" stroke="#21776b" strokeWidth={2} />
                <Line type="monotone" dataKey="expense" name="Chi" stroke="#c47a1a" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card>
          <CardHeader>
            <div>
              <CardTitle>Chi tiêu theo thành viên</CardTitle>
              <CardDescription>Tháng hiện tại</CardDescription>
            </div>
          </CardHeader>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data.spendingByMember}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e3e9ef" />
                <XAxis dataKey="memberName" hide />
                <YAxis tickFormatter={(v) => `${Math.round(Number(v) / 1_000_000)}tr`} width={40} />
                <Tooltip formatter={(v) => formatVnd(Number(v))} />
                <Bar dataKey="amount" name="Chi tiêu" fill="#4aaf9e" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-2 space-y-1 text-sm">
            {data.spendingByMember.map((m) => (
              <div key={m.memberName} className="flex justify-between">
                <span className="text-muted">{m.memberName}</span>
                <span className="font-medium">{formatVnd(m.amount)}</span>
              </div>
            ))}
          </div>
        </Card>
      </div>

      <div className="mt-4 grid gap-4 xl:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Theo danh mục</CardTitle>
          </CardHeader>
          <div className="space-y-3">
            {data.spendingByCategory.map((c) => (
              <div key={c.category}>
                <div className="mb-1 flex justify-between text-sm">
                  <span>{c.category}</span>
                  <span className="font-medium">{formatVnd(c.amount)}</span>
                </div>
                <Progress
                  value={c.amount}
                  max={Math.max(...data.spendingByCategory.map((x) => x.amount))}
                />
              </div>
            ))}
          </div>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Ngân sách vs thực tế</CardTitle>
          </CardHeader>
          <div className="space-y-3">
            {data.budget.categories.map((c) => (
              <div key={c.id} className="rounded-xl border border-line p-3">
                <div className="mb-2 flex justify-between text-sm">
                  <span className="font-medium">{c.name}</span>
                  <span className={c.spent > c.allocated ? 'text-danger' : 'text-muted'}>
                    {formatVnd(c.spent)} / {formatVnd(c.allocated)}
                  </span>
                </div>
                <Progress value={c.spent} max={c.allocated} />
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  )
}
