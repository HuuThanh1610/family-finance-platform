import { Bell, Menu, Search } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/features/auth/AuthContext'

interface TopbarProps {
  onMenuClick: () => void
}

export function Topbar({ onMenuClick }: TopbarProps) {
  const { session, logout } = useAuth()

  const initials =
    session?.user.fullName
      ?.split(' ')
      .slice(-2)
      .map((p) => p[0])
      .join('') ?? 'U'

  return (
    <header className="sticky top-0 z-30 flex h-12 items-center justify-between gap-3 border-b border-border bg-bg px-3 md:px-4">
      <div className="flex min-w-0 items-center gap-2">
        <Button variant="ghost" size="sm" className="lg:hidden" onClick={onMenuClick}>
          <Menu className="h-4 w-4" strokeWidth={1.75} />
        </Button>
        <div className="hidden h-8 max-w-sm items-center gap-2 rounded-[2px] border border-border bg-bg px-2.5 text-[13px] text-fg-muted md:flex">
          <Search className="h-3.5 w-3.5" strokeWidth={1.75} />
          <span className="truncate">Tìm giao dịch, thành viên...</span>
        </div>
      </div>

      <div className="flex items-center gap-1.5">
        <Link to="/notifications">
          <Button variant="ghost" size="sm" aria-label="Thông báo">
            <Bell className="h-4 w-4" strokeWidth={1.75} />
          </Button>
        </Link>
        <div className="hidden text-right sm:block">
          <p className="text-[13px] font-medium text-fg">{session?.user.fullName}</p>
          <p className="text-[11px] text-fg-muted">{session?.mock ? 'Phiên mock' : 'Đã đăng nhập'}</p>
        </div>
        <div className="flex h-7 w-7 items-center justify-center rounded-[2px] border border-border bg-bg-tertiary text-[11px] font-medium text-fg">
          {initials}
        </div>
        <Button variant="ghost" size="sm" onClick={logout}>
          Đăng xuất
        </Button>
      </div>
    </header>
  )
}
