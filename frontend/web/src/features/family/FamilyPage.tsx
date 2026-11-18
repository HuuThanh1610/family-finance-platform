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
import { Table, TBody, TD, TH, THead, TR } from '@/components/ui/table'
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
        description="Thành viên, vai trò và quyền hạn."
        actions={<Button onClick={() => setInviteOpen(true)}>Mời thành viên</Button>}
      />

      <div className="grid gap-3 lg:grid-cols-3">
        <Card className="lg:col-span-2 !p-0 overflow-hidden">
          <div className="border-b border-border px-4 py-3">
            <CardTitle>Thành viên</CardTitle>
          </div>
          <Table>
            <THead>
              <TR>
                <TH>Thành viên</TH>
                <TH>Vai trò</TH>
                <TH>Tham gia</TH>
                <TH className="text-right">Chi tiêu tháng</TH>
              </TR>
            </THead>
            <TBody>
              {data.members.map((member) => (
                <TR
                  key={member.id}
                  className={`cursor-pointer ${selected?.id === member.id ? 'bg-bg-tertiary' : ''}`}
                  onClick={() => setSelectedId(member.id)}
                >
                  <TD className="!tabular-nums-none">
                    <div className="flex items-center gap-2">
                      <span className="flex h-6 w-6 items-center justify-center rounded-[2px] border border-border bg-bg-tertiary text-[10px] font-medium">
                        {member.avatarInitials}
                      </span>
                      <div>
                        <p className="font-medium">{member.fullName}</p>
                        <p className="text-[12px] text-fg-muted">{member.email}</p>
                      </div>
                    </div>
                  </TD>
                  <TD>
                    <Badge variant="muted">{member.role}</Badge>
                  </TD>
                  <TD className="text-fg-secondary">{formatDate(member.joinedAt)}</TD>
                  <TD className="text-right font-medium">{formatVnd(member.monthlySpend)}</TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </Card>

        <div className="space-y-3">
          {selected ? (
            <Card>
              <CardHeader>
                <CardTitle>Hồ sơ</CardTitle>
              </CardHeader>
              <dl className="space-y-2 text-[13px]">
                <div className="flex justify-between gap-2">
                  <dt className="text-fg-secondary">Họ tên</dt>
                  <dd className="font-medium">{selected.fullName}</dd>
                </div>
                <div className="flex justify-between gap-2">
                  <dt className="text-fg-secondary">Email</dt>
                  <dd className="truncate font-medium">{selected.email}</dd>
                </div>
                <div className="flex justify-between gap-2">
                  <dt className="text-fg-secondary">Vai trò</dt>
                  <dd>
                    <Badge variant="muted">{selected.role}</Badge>
                  </dd>
                </div>
                <div className="flex justify-between gap-2">
                  <dt className="text-fg-secondary">Chi tiêu</dt>
                  <dd className="font-medium tabular-nums">{formatVnd(selected.monthlySpend)}</dd>
                </div>
              </dl>
            </Card>
          ) : null}

          <Card>
            <CardHeader>
              <CardTitle>Quyền theo vai trò</CardTitle>
            </CardHeader>
            <div className="space-y-3">
              {(Object.keys(rolePermissions) as FamilyRole[]).map((role) => (
                <div key={role}>
                  <Badge variant="muted" className="mb-1">
                    {role}
                  </Badge>
                  <ul className="list-inside list-disc text-[12px] text-fg-secondary">
                    {rolePermissions[role].map((p) => (
                      <li key={p}>{p}</li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
            <p className="mt-3 text-[11px] text-fg-muted">
              Resource-level auth sẽ triển khai với Keycloak (learning task).
            </p>
          </Card>
        </div>
      </div>

      <Dialog
        open={inviteOpen}
        onClose={() => setInviteOpen(false)}
        title="Mời thành viên"
        description="Tạo lời mời (mô phỏng)."
      >
        <form
          className="space-y-3"
          onSubmit={form.handleSubmit((values) => {
            setInviteMsg(`Đã tạo lời mời tới ${values.email} · ${values.role} (mock).`)
            form.reset()
          })}
        >
          <div>
            <Label htmlFor="invite-email">Email</Label>
            <Input id="invite-email" {...form.register('email')} />
            {form.formState.errors.email ? (
              <p className="mt-1 text-[12px] text-danger">{form.formState.errors.email.message}</p>
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
          {inviteMsg ? <p className="text-[13px] text-fg">{inviteMsg}</p> : null}
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
