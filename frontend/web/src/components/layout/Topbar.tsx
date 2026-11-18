import { Bell, Menu, Search } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/features/auth/AuthContext'

interface TopbarProps {
  onMenuClick: () => void
}

export function Topbar({ onMenuClick }: TopbarProps) {
  const { session, logout } = useAuth()

  return (
    <header className="sticky top-0 z-30 flex items-center justify-between gap-3 border-b border-line bg-white/80 px-4 py-3 backdrop-blur md:px-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" className="lg:hidden" onClick={onMenuClick}>
          <Menu className="h-5 w-5" />
        </Button>
        <div className="hidden items-center gap-2 rounded-xl border border-line bg-white px-3 py-2 text-sm text-muted md:flex">
          <Search className="h-4 w-4" />
          <span>Tìm giao dịch, thành viên, ngân sách...</span>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <Link to="/notifications">
          <Button variant="secondary" size="sm" aria-label="Thông báo">
            <Bell className="h-4 w-4" />
          </Button>
        </Link>
        <div className="hidden text-right sm:block">
          <p className="text-sm font-semibold text-ink">{session?.user.fullName}</p>
          <p className="text-xs text-muted">{session?.mock ? 'Phiên mock' : 'Đã đăng nhập'}</p>
        </div>
        <div className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-100 text-sm font-bold text-brand-800">
          {session?.user.fullName
            ?.split(' ')
            .slice(-2)
            .map((p) => p[0])
            .join('') ?? 'U'}
        </div>
        <Button variant="ghost" size="sm" onClick={logout}>
          Đăng xuất
        </Button>
      </div>
    </header>
  )
}
