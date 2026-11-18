import { useMemo, useState } from 'react'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Card, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog } from '@/components/ui/dialog'
import { useAsync } from '@/hooks/useAsync'
import { formatDateTime, formatVnd } from '@/lib/utils'
import { walletApi } from '@/services/family-finance-api'
import type { WalletTransaction } from '@/types/domain'

export function WalletsPage() {
  const wallets = useAsync(() => walletApi.listWallets(), [])
  const txns = useAsync(() => walletApi.getTransactions(), [])
  const [selectedWalletId, setSelectedWalletId] = useState<string | null>(null)
  const [selectedTxn, setSelectedTxn] = useState<WalletTransaction | null>(null)

  const activeWalletId = selectedWalletId ?? wallets.data?.[0]?.id ?? null
  const activeWallet = wallets.data?.find((w) => w.id === activeWalletId)
  const filteredTxns = useMemo(
    () => (activeWalletId ? txns.data?.filter((t) => t.walletId === activeWalletId) ?? [] : []),
    [txns.data, activeWalletId],
  )

  if (wallets.loading || txns.loading) return <LoadingState />
  if (wallets.error || txns.error || !wallets.data) {
    return <ErrorState message={wallets.error ?? txns.error ?? undefined} onRetry={wallets.reload} />
  }

  return (
    <div>
      <PageHeader
        title="Quản lý ví"
        description="Ví chung và ví cá nhân. Số dư hiển thị từ dữ liệu demo — không phát sinh bút toán ledger thật."
      />

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {wallets.data.map((wallet) => (
          <button
            key={wallet.id}
            type="button"
            onClick={() => setSelectedWalletId(wallet.id)}
            className="text-left"
          >
            <Card
              className={
                wallet.id === activeWalletId ? 'ring-2 ring-brand-500' : 'hover:border-brand-300'
              }
            >
              <p className="text-sm text-muted">{wallet.name}</p>
              <p className="mt-2 font-display text-xl font-extrabold">{formatVnd(wallet.balance)}</p>
              <Badge className="mt-3" variant={wallet.type === 'FAMILY_SHARED' ? 'default' : 'muted'}>
                {wallet.type === 'FAMILY_SHARED' ? 'Ví chung' : 'Ví cá nhân'}
              </Badge>
            </Card>
          </button>
        ))}
      </div>

      <Card className="mt-6">
        <CardHeader>
          <div>
            <CardTitle>Lịch sử giao dịch — {activeWallet?.name}</CardTitle>
            <p className="text-sm text-muted">Phân biệt ghi nợ (debit) / ghi có (credit)</p>
          </div>
        </CardHeader>
        <div className="overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="border-b border-line text-muted">
              <tr>
                <th className="px-2 py-2 font-medium">Thời gian</th>
                <th className="px-2 py-2 font-medium">Mô tả</th>
                <th className="px-2 py-2 font-medium">Loại</th>
                <th className="px-2 py-2 font-medium text-right">Số tiền</th>
              </tr>
            </thead>
            <tbody>
              {filteredTxns.map((txn) => (
                <tr
                  key={txn.id}
                  className="cursor-pointer border-b border-line/70 hover:bg-brand-50/40"
                  onClick={() => setSelectedTxn(txn)}
                >
                  <td className="px-2 py-3 whitespace-nowrap">{formatDateTime(txn.occurredAt)}</td>
                  <td className="px-2 py-3">
                    <p className="font-medium">{txn.description}</p>
                    <p className="text-xs text-muted">{txn.category}</p>
                  </td>
                  <td className="px-2 py-3">
                    <Badge variant={txn.entryType === 'CREDIT' ? 'success' : 'muted'}>
                      {txn.entryType}
                    </Badge>
                  </td>
                  <td
                    className={`px-2 py-3 text-right font-semibold ${
                      txn.entryType === 'CREDIT' ? 'text-success' : 'text-ink'
                    }`}
                  >
                    {txn.entryType === 'CREDIT' ? '+' : '-'}
                    {formatVnd(txn.amount)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Dialog
        open={Boolean(selectedTxn)}
        onClose={() => setSelectedTxn(null)}
        title="Chi tiết giao dịch"
      >
        {selectedTxn ? (
          <div className="space-y-2 text-sm">
            <p>
              <span className="text-muted">Mã:</span> {selectedTxn.id}
            </p>
            <p>
              <span className="text-muted">Mô tả:</span> {selectedTxn.description}
            </p>
            <p>
              <span className="text-muted">Số tiền:</span> {formatVnd(selectedTxn.amount)}
            </p>
            <p>
              <span className="text-muted">Loại bút toán:</span> {selectedTxn.entryType}
            </p>
            <p>
              <span className="text-muted">Thành viên:</span> {selectedTxn.memberName ?? '—'}
            </p>
            <p>
              <span className="text-muted">Tham chiếu:</span> {selectedTxn.referenceId ?? '—'}
            </p>
            <p className="rounded-lg bg-slate-50 p-3 text-xs text-muted">
              Double-entry posting và cập nhật số dư atomic sẽ do owner triển khai ở wallet-service.
            </p>
          </div>
        ) : null}
      </Dialog>
    </div>
  )
}
