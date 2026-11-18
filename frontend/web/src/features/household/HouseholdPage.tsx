import { useState } from 'react'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle } from '@/components/ui/card'
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
      <PageHeader
        title="Gia dụng & mua sắm"
        description="Theo dõi tồn kho và danh sách mua sắm của hộ gia đình."
      />

      <div className="grid gap-4 xl:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Tồn kho</CardTitle>
          </CardHeader>
          <div className="space-y-3">
            {inventory.data.map((item) => {
              const low = item.quantity < item.minThreshold
              return (
                <div
                  key={item.id}
                  className="flex items-center justify-between gap-3 rounded-xl border border-line px-3 py-3"
                >
                  <div>
                    <p className="font-semibold">{item.name}</p>
                    <p className="text-xs text-muted">
                      {item.category} · cập nhật {formatDate(item.updatedAt)}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-semibold">
                      {item.quantity} {item.unit}
                    </p>
                    <Badge variant={low ? 'danger' : 'muted'}>
                      ngưỡng {item.minThreshold} {item.unit}
                    </Badge>
                  </div>
                </div>
              )
            })}
          </div>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Danh sách mua sắm</CardTitle>
          </CardHeader>
          <div className="space-y-3">
            {list.map((item) => (
              <div
                key={item.id}
                className="flex items-center justify-between gap-3 rounded-xl border border-line px-3 py-3"
              >
                <div>
                  <p className={`font-semibold ${item.purchased ? 'line-through text-muted' : ''}`}>
                    {item.name}
                  </p>
                  <p className="text-xs text-muted">
                    {item.quantity} {item.unit}
                    {item.assignedTo ? ` · giao ${item.assignedTo}` : ''}
                  </p>
                </div>
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
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  )
}
