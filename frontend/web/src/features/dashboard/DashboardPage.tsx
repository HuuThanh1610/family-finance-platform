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
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
import { useAsync } from '@/hooks/useAsync'
import { formatDateTime, formatVnd } from '@/lib/utils'
import { dashboardApi } from '@/services/family-finance-api'

const CHART_GRAYS = ['#171717', '#525252', '#737373', '#a3a3a3', '#d4d4d4', '#e5e5e5', '#f5f5f5']

export function DashboardPage() {
  const { data, loading, error, reload } = useAsync(() => dashboardApi.getSummary(), [])

  if (loading) return <LoadingState />
  if (error || !data) return <ErrorState message={error ?? undefined} onRetry={reload} />

  return (
    <div>
      <PageHeader
        title="Tổng quan"
        description="Số dư, ngân sách và hoạt động gần đây (dữ liệu demo)."
      />

      <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
        <Metric label="Tổng số dư" value={formatVnd(data.totalBalance)} hint="Tất cả ví" />
        <Metric label="Thu nhập tháng" value={formatVnd(data.monthlyIncome)} hint="Tháng 11/2026" />
        <Metric label="Chi tiêu tháng" value={formatVnd(data.monthlyExpense)} hint="Đã ghi nhận" />
        <Metric
          label="Ngân sách còn lại"
          value={formatVnd(data.remainingBudget)}
          hint={data.remainingBudget < 0 ? 'Vượt hạn mức' : 'Trong hạn mức'}
          warn={data.remainingBudget < 0}
        />
      </div>

      <div className="mt-4 grid gap-3 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader>
            <div>
              <CardTitle>Chi tiêu theo tháng</CardTitle>
              <CardDescription>6 tháng gần nhất</CardDescription>
            </div>
          </CardHeader>
          <div className="h-56">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data.monthlySpending}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e5e5" />
                <XAxis dataKey="label" tickLine={false} axisLine={false} tick={{ fill: '#737373', fontSize: 12 }} />
                <YAxis
                  tickFormatter={(v) => `${Math.round(Number(v) / 1_000_000)}tr`}
                  tickLine={false}
                  axisLine={false}
                  width={36}
                  tick={{ fill: '#737373', fontSize: 12 }}
                />
                <Tooltip formatter={(value) => formatVnd(Number(value))} />
                <Bar dataKey="amount" fill="#171717" radius={[2, 2, 0, 0]} />
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
          <div className="h-56">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={data.spendingByCategory}
                  dataKey="amount"
                  nameKey="category"
                  innerRadius={48}
                  outerRadius={78}
                  paddingAngle={1}
                  stroke="#fff"
                  strokeWidth={1}
                >
                  {data.spendingByCategory.map((entry, index) => (
                    <Cell key={entry.category} fill={CHART_GRAYS[index % CHART_GRAYS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => formatVnd(Number(value))} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <div className="mt-4 grid gap-3 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader>
            <CardTitle>Giao dịch gần đây</CardTitle>
            <Link to="/wallets" className="text-[12px] font-medium text-fg-secondary hover:text-fg">
              Xem ví
            </Link>
          </CardHeader>
          <Table>
            <THead>
              <TR>
                <TH>Mô tả</TH>
                <TH>Thời gian</TH>
                <TH className="text-right">Số tiền</TH>
              </TR>
            </THead>
            <TBody>
              {data.recentTransactions.map((txn) => (
                <TR key={txn.id}>
                  <TD className="!tabular-nums-none">
                    <p className="font-medium text-fg">{txn.description}</p>
                    <p className="text-[12px] text-fg-muted">{txn.memberName}</p>
                  </TD>
                  <TD className="whitespace-nowrap text-fg-secondary">{formatDateTime(txn.occurredAt)}</TD>
                  <TD className={`text-right font-medium ${txn.entryType === 'CREDIT' ? 'text-success' : 'text-fg'}`}>
                    {txn.entryType === 'CREDIT' ? '+' : '-'}
                    {formatVnd(txn.amount)}
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </Card>

        <div className="space-y-3">
          <Card>
            <CardHeader>
              <CardTitle>Chi tiêu thành viên</CardTitle>
            </CardHeader>
            <div className="space-y-2 text-[13px]">
              {data.memberSpending.map((m) => (
                <div key={m.memberName} className="flex items-center justify-between gap-2">
                  <span className="text-fg-secondary">{m.memberName}</span>
                  <span className="font-medium tabular-nums">{formatVnd(m.amount)}</span>
                </div>
              ))}
            </div>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Chờ duyệt</CardTitle>
              <Link to="/allowances" className="text-[12px] font-medium text-fg-secondary hover:text-fg">
                Xem
              </Link>
            </CardHeader>
            <div className="space-y-2">
              {data.pendingAllowances.map((req) => (
                <div key={req.id} className="border border-border px-2.5 py-2 text-[13px]">
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-medium">{req.childName}</span>
                    <Badge variant="warning">PENDING</Badge>
                  </div>
                  <p className="mt-1 text-fg-secondary">
                    {formatVnd(req.amount)} — {req.reason}
                  </p>
                </div>
              ))}
            </div>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Tồn kho thấp</CardTitle>
              <Link to="/household" className="text-[12px] font-medium text-fg-secondary hover:text-fg">
                Kho
              </Link>
            </CardHeader>
            <div className="space-y-2 text-[13px]">
              {data.lowStockItems.map((item) => (
                <div key={item.id} className="flex items-center justify-between gap-2">
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

function Metric({
  label,
  value,
  hint,
  warn,
}: {
  label: string
  value: string
  hint: string
  warn?: boolean
}) {
  return (
    <Card className="!p-3">
      <p className="text-[12px] text-fg-secondary">{label}</p>
      <p className={`mt-1 text-[18px] font-semibold tabular-nums ${warn ? 'text-danger' : 'text-fg'}`}>
        {value}
      </p>
      <p className="mt-0.5 text-[11px] text-fg-muted">{hint}</p>
    </Card>
  )
}
