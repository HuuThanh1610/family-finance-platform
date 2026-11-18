import { useMemo, useState } from 'react'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Card, CardTitle } from '@/components/ui/card'
import { Dialog } from '@/components/ui/dialog'
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
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
        title="Ví"
        description="Ví chung và ví cá nhân — số dư demo, chưa phát sinh bút toán ledger thật."
      />

      <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-4">
        {wallets.data.map((wallet) => (
          <button
            key={wallet.id}
            type="button"
            onClick={() => setSelectedWalletId(wallet.id)}
            className="text-left"
          >
            <Card
              className={`!p-3 ${
                wallet.id === activeWalletId ? 'border-fg' : 'hover:border-border-strong'
              }`}
            >
              <p className="text-[12px] text-fg-secondary">{wallet.name}</p>
              <p className="mt-1 text-[18px] font-semibold tabular-nums">{formatVnd(wallet.balance)}</p>
              <Badge className="mt-2" variant="muted">
                {wallet.type === 'FAMILY_SHARED' ? 'Ví chung' : 'Ví cá nhân'}
              </Badge>
            </Card>
          </button>
        ))}
      </div>

      <Card className="mt-3 !p-0 overflow-hidden">
        <div className="border-b border-border px-4 py-3">
          <CardTitle>Lịch sử — {activeWallet?.name}</CardTitle>
          <p className="mt-0.5 text-[12px] text-fg-secondary">DEBIT / CREDIT</p>
        </div>
        <Table>
          <THead>
            <TR>
              <TH>Thời gian</TH>
              <TH>Mô tả</TH>
              <TH>Loại</TH>
              <TH className="text-right">Số tiền</TH>
            </TR>
          </THead>
          <TBody>
            {filteredTxns.map((txn) => (
              <TR key={txn.id} className="cursor-pointer" onClick={() => setSelectedTxn(txn)}>
                <TD className="whitespace-nowrap text-fg-secondary">{formatDateTime(txn.occurredAt)}</TD>
                <TD className="!tabular-nums-none">
                  <p className="font-medium">{txn.description}</p>
                  <p className="text-[12px] text-fg-muted">{txn.category}</p>
                </TD>
                <TD>
                  <Badge variant={txn.entryType === 'CREDIT' ? 'success' : 'muted'}>{txn.entryType}</Badge>
                </TD>
                <TD
                  className={`text-right font-medium ${
                    txn.entryType === 'CREDIT' ? 'text-success' : 'text-fg'
                  }`}
                >
                  {txn.entryType === 'CREDIT' ? '+' : '-'}
                  {formatVnd(txn.amount)}
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
      </Card>

      <Dialog open={Boolean(selectedTxn)} onClose={() => setSelectedTxn(null)} title="Chi tiết giao dịch">
        {selectedTxn ? (
          <dl className="space-y-2 text-[13px]">
            <Row label="Mã" value={selectedTxn.id} />
            <Row label="Mô tả" value={selectedTxn.description} />
            <Row label="Số tiền" value={formatVnd(selectedTxn.amount)} />
            <Row label="Loại" value={selectedTxn.entryType} />
            <Row label="Thành viên" value={selectedTxn.memberName ?? '—'} />
            <Row label="Tham chiếu" value={selectedTxn.referenceId ?? '—'} />
            <p className="border border-border bg-bg-secondary p-2.5 text-[12px] text-fg-secondary">
              Double-entry posting sẽ do owner triển khai ở wallet-service.
            </p>
          </dl>
        ) : null}
      </Dialog>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-3">
      <dt className="text-fg-secondary">{label}</dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  )
}
