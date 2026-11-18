import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { Link } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Card, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAsync } from '@/hooks/useAsync'
import { formatDateTime, formatVnd } from '@/lib/utils'
import { dashboardApi } from '@/services/family-finance-api'

const PIE_COLORS = ['#21776b', '#4aaf9e', '#7ccbbb', '#c47a1a', '#5b6b78', '#1f7a4d', '#c43c3c']

export function DashboardPage() {
  const { data, loading, error, reload } = useAsync(() => dashboardApi.getSummary(), [])

  if (loading) return <LoadingState />
  if (error || !data) return <ErrorState message={error ?? undefined} onRetry={reload} />

  return (
    <div>
      <PageHeader
        title="Tổng quan gia đình"
        description="Số dư, ngân sách và hoạt động gần đây của Gia đình Nguyễn (dữ liệu demo)."
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Tổng số dư" value={formatVnd(data.totalBalance)} hint="Tất cả ví" />
        <StatCard label="Thu nhập tháng này" value={formatVnd(data.monthlyIncome)} hint="Tháng 11/2026" />
        <StatCard label="Chi tiêu tháng này" value={formatVnd(data.monthlyExpense)} hint="Đã ghi nhận" />
        <StatCard
          label="Ngân sách còn lại"
          value={formatVnd(data.remainingBudget)}
          hint={data.remainingBudget < 0 ? 'Đã vượt hạn mức' : 'Trong hạn mức'}
        />
      </div>

      <div className="mt-6 grid gap-4 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader>
            <div>
              <CardTitle>Chi tiêu theo tháng</CardTitle>
              <CardDescription>6 tháng gần nhất</CardDescription>
            </div>
          </CardHeader>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data.monthlySpending}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e3e9ef" />
                <XAxis dataKey="label" tickLine={false} axisLine={false} />
                <YAxis
                  tickFormatter={(v) => `${Math.round(Number(v) / 1_000_000)}tr`}
                  tickLine={false}
                  axisLine={false}
                  width={40}
                />
                <Tooltip formatter={(value) => formatVnd(Number(value))} />
                <Bar dataKey="amount" fill="#21776b" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <Card>
          <CardHeader>
            <div>
              <CardTitle>Theo danh mục</CardTitle>
              <CardDescription>Tháng hiện tại</CardDescription>
            </div>
          </CardHeader>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={data.spendingByCategory}
                  dataKey="amount"
                  nameKey="category"
                  innerRadius={55}
                  outerRadius={90}
                  paddingAngle={2}
                >
                  {data.spendingByCategory.map((entry, index) => (
                    <Cell key={entry.category} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => formatVnd(Number(value))} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <div className="mt-6 grid gap-4 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader>
            <CardTitle>Giao dịch gần đây</CardTitle>
            <Link to="/wallets" className="text-sm font-semibold text-brand-700">
              Xem ví
            </Link>
          </CardHeader>
          <div className="space-y-3">
            {data.recentTransactions.map((txn) => (
              <div
                key={txn.id}
                className="flex items-center justify-between gap-3 rounded-xl border border-line px-3 py-3"
              >
                <div>
                  <p className="font-medium text-ink">{txn.description}</p>
                  <p className="text-xs text-muted">
                    {txn.memberName} · {formatDateTime(txn.occurredAt)}
                  </p>
                </div>
                <p
                  className={
                    txn.entryType === 'CREDIT' ? 'font-semibold text-success' : 'font-semibold text-ink'
                  }
                >
                  {txn.entryType === 'CREDIT' ? '+' : '-'}
                  {formatVnd(txn.amount)}
                </p>
              </div>
            ))}
          </div>
        </Card>

        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Chi tiêu thành viên</CardTitle>
            </CardHeader>
            <div className="space-y-3">
              {data.memberSpending.map((m) => (
                <div key={m.memberName} className="flex items-center justify-between text-sm">
                  <span className="text-muted">{m.memberName}</span>
                  <span className="font-semibold">{formatVnd(m.amount)}</span>
                </div>
              ))}
            </div>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Duyệt tiền tiêu vặt</CardTitle>
              <Link to="/allowances" className="text-sm font-semibold text-brand-700">
                Xem
              </Link>
            </CardHeader>
            <div className="space-y-2">
              {data.pendingAllowances.map((req) => (
                <div key={req.id} className="rounded-xl bg-amber-50 px-3 py-2 text-sm">
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-medium">{req.childName}</span>
                    <Badge variant="warning">Chờ duyệt</Badge>
                  </div>
                  <p className="mt-1 text-muted">
                    {formatVnd(req.amount)} — {req.reason}
                  </p>
                </div>
              ))}
            </div>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Cảnh báo tồn kho</CardTitle>
              <Link to="/household" className="text-sm font-semibold text-brand-700">
                Kho
              </Link>
            </CardHeader>
            <div className="space-y-2">
              {data.lowStockItems.map((item) => (
                <div key={item.id} className="flex items-center justify-between text-sm">
                  <span>{item.name}</span>
                  <Badge variant="danger">
                    {item.quantity}/{item.minThreshold} {item.unit}
                  </Badge>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}

function StatCard({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <Card>
      <p className="text-sm text-muted">{label}</p>
      <p className="mt-2 font-display text-2xl font-extrabold text-ink">{value}</p>
      <p className="mt-1 text-xs text-muted">{hint}</p>
    </Card>
  )
}
