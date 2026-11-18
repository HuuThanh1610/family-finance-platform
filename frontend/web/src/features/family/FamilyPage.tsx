import { useMemo, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select } from '@/components/ui/select'
import { useAsync } from '@/hooks/useAsync'
import { formatDate, formatVnd } from '@/lib/utils'
import { rolePermissions } from '@/mocks/data'
import { familyApi } from '@/services/family-finance-api'
import type { FamilyRole } from '@/types/domain'

const inviteSchema = z.object({
  email: z.email('Email không hợp lệ'),
  role: z.enum(['PARENT', 'ADULT_MEMBER', 'CHILD', 'VIEWER']),
})

type InviteValues = z.infer<typeof inviteSchema>

const roleBadge: Record<FamilyRole, 'default' | 'info' | 'success' | 'warning' | 'muted'> = {
  OWNER: 'default',
  PARENT: 'info',
  ADULT_MEMBER: 'success',
  CHILD: 'warning',
  VIEWER: 'muted',
}

export function FamilyPage() {
  const { data, loading, error, reload } = useAsync(() => familyApi.getFamily(), [])
  const [inviteOpen, setInviteOpen] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [inviteMsg, setInviteMsg] = useState<string | null>(null)

  const selected = useMemo(
    () => data?.members.find((m) => m.id === selectedId) ?? data?.members[0],
    [data, selectedId],
  )

  const form = useForm<InviteValues>({
    resolver: zodResolver(inviteSchema),
    defaultValues: { email: '', role: 'ADULT_MEMBER' },
  })

  if (loading) return <LoadingState />
  if (error || !data) return <ErrorState message={error ?? undefined} onRetry={reload} />

  return (
    <div>
      <PageHeader
        title={data.name}
        description="Thành viên, vai trò và quyền hạn trong gia đình."
        actions={
          <Button onClick={() => setInviteOpen(true)}>Mời thành viên</Button>
        }
      />

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Danh sách thành viên</CardTitle>
          </CardHeader>
          <div className="space-y-3">
            {data.members.map((member) => (
              <button
                key={member.id}
                type="button"
                onClick={() => setSelectedId(member.id)}
                className="flex w-full items-center justify-between gap-3 rounded-xl border border-line px-3 py-3 text-left hover:bg-brand-50/50"
              >
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-100 text-sm font-bold text-brand-800">
                    {member.avatarInitials}
                  </div>
                  <div>
                    <p className="font-semibold text-ink">{member.fullName}</p>
                    <p className="text-xs text-muted">{member.email}</p>
                  </div>
                </div>
                <div className="text-right">
                  <Badge variant={roleBadge[member.role]}>{member.role}</Badge>
                  <p className="mt-1 text-xs text-muted">Tham gia {formatDate(member.joinedAt)}</p>
                </div>
              </button>
            ))}
          </div>
        </Card>

        <div className="space-y-4">
          {selected ? (
            <Card>
              <CardHeader>
                <CardTitle>Hồ sơ thành viên</CardTitle>
              </CardHeader>
              <div className="space-y-2 text-sm">
                <p>
                  <span className="text-muted">Họ tên:</span> {selected.fullName}
                </p>
                <p>
                  <span className="text-muted">Email:</span> {selected.email}
                </p>
                <p>
                  <span className="text-muted">Vai trò:</span>{' '}
                  <Badge variant={roleBadge[selected.role]}>{selected.role}</Badge>
                </p>
                <p>
                  <span className="text-muted">Chi tiêu tháng:</span>{' '}
                  {formatVnd(selected.monthlySpend)}
                </p>
              </div>
            </Card>
          ) : null}

          <Card>
            <CardHeader>
              <CardTitle>Ma trận quyền (UI)</CardTitle>
            </CardHeader>
            <div className="space-y-3">
              {(Object.keys(rolePermissions) as FamilyRole[]).map((role) => (
                <div key={role}>
                  <Badge variant={roleBadge[role]} className="mb-1">
                    {role}
                  </Badge>
                  <ul className="list-inside list-disc text-xs text-muted">
                    {rolePermissions[role].map((p) => (
                      <li key={p}>{p}</li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
            <p className="mt-3 text-xs text-amber-700">
              Phân quyền resource-level sẽ được triển khai với Keycloak + Spring Security (learning task).
            </p>
          </Card>
        </div>
      </div>

      <Dialog
        open={inviteOpen}
        onClose={() => setInviteOpen(false)}
        title="Mời thành viên"
        description="Gửi lời mời tham gia gia đình (mô phỏng)."
      >
        <form
          className="space-y-4"
          onSubmit={form.handleSubmit((values) => {
            setInviteMsg(
              `Đã tạo lời mời tới ${values.email} với vai trò ${values.role} (mock — chưa gửi email thật).`,
            )
            form.reset()
          })}
        >
          <div>
            <Label htmlFor="invite-email">Email</Label>
            <Input id="invite-email" {...form.register('email')} />
            {form.formState.errors.email ? (
              <p className="mt-1 text-xs text-danger">{form.formState.errors.email.message}</p>
            ) : null}
          </div>
          <div>
            <Label htmlFor="invite-role">Vai trò</Label>
            <Select id="invite-role" {...form.register('role')}>
              <option value="PARENT">PARENT</option>
              <option value="ADULT_MEMBER">ADULT_MEMBER</option>
              <option value="CHILD">CHILD</option>
              <option value="VIEWER">VIEWER</option>
            </Select>
          </div>
          {inviteMsg ? <p className="text-sm text-brand-700">{inviteMsg}</p> : null}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={() => setInviteOpen(false)}>
              Đóng
            </Button>
            <Button type="submit">Gửi lời mời</Button>
          </div>
        </form>
      </Dialog>
    </div>
  )
}
