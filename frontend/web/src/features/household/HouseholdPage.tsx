import { useState } from 'react'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardTitle } from '@/components/ui/card'
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
import { useAsync } from '@/hooks/useAsync'
import { formatDate } from '@/lib/utils'
import { householdApi } from '@/services/family-finance-api'

export function HouseholdPage() {
  const inventory = useAsync(() => householdApi.listInventory(), [])
  const shopping = useAsync(() => householdApi.listShopping(), [])
  const [overrides, setOverrides] = useState<Record<string, boolean>>({})

  if (inventory.loading || shopping.loading) return <LoadingState />
  if (inventory.error || shopping.error || !inventory.data || !shopping.data) {
    return (
      <ErrorState
        message={inventory.error ?? shopping.error ?? undefined}
        onRetry={() => {
          inventory.reload()
          shopping.reload()
        }}
      />
    )
  }

  const list = shopping.data.map((item) => ({
    ...item,
    purchased: overrides[item.id] ?? item.purchased,
  }))

  return (
    <div>
      <PageHeader title="Gia dụng" description="Tồn kho và danh sách mua sắm." />

      <div className="grid gap-3 xl:grid-cols-2">
        <Card className="!p-0 overflow-hidden">
          <div className="border-b border-border px-4 py-3">
            <CardTitle>Tồn kho</CardTitle>
          </div>
          <Table>
            <THead>
              <TR>
                <TH>Sản phẩm</TH>
                <TH>Danh mục</TH>
                <TH className="text-right">Số lượng</TH>
                <TH>Trạng thái</TH>
              </TR>
            </THead>
            <TBody>
              {inventory.data.map((item) => {
                const low = item.quantity < item.minThreshold
                return (
                  <TR key={item.id}>
                    <TD className="!tabular-nums-none">
                      <p className="font-medium">{item.name}</p>
                      <p className="text-[12px] text-fg-muted">cập nhật {formatDate(item.updatedAt)}</p>
                    </TD>
                    <TD className="text-fg-secondary">{item.category}</TD>
                    <TD className="text-right font-medium">
                      {item.quantity} {item.unit}
                    </TD>
                    <TD>
                      <Badge variant={low ? 'danger' : 'muted'}>
                        ngưỡng {item.minThreshold}
                      </Badge>
                    </TD>
                  </TR>
                )
              })}
            </TBody>
          </Table>
        </Card>

        <Card className="!p-0 overflow-hidden">
          <div className="border-b border-border px-4 py-3">
            <CardTitle>Danh sách mua sắm</CardTitle>
          </div>
          <Table>
            <THead>
              <TR>
                <TH>Mặt hàng</TH>
                <TH>Giao</TH>
                <TH className="text-right">SL</TH>
                <TH />
              </TR>
            </THead>
            <TBody>
              {list.map((item) => (
                <TR key={item.id}>
                  <TD className={`!tabular-nums-none ${item.purchased ? 'text-fg-muted line-through' : ''}`}>
                    <span className="font-medium">{item.name}</span>
                  </TD>
                  <TD className="text-fg-secondary">{item.assignedTo ?? '—'}</TD>
                  <TD className="text-right">
                    {item.quantity} {item.unit}
                  </TD>
                  <TD className="text-right">
                    <Button
                      size="sm"
                      variant={item.purchased ? 'secondary' : 'primary'}
                      onClick={() =>
                        setOverrides((prev) => ({
                          ...prev,
                          [item.id]: !item.purchased,
                        }))
                      }
                    >
                      {item.purchased ? 'Hoàn tác' : 'Đã mua'}
                    </Button>
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </Card>
      </div>
    </div>
  )
}
