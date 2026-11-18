import { NavLink } from 'react-router-dom'
import {
  Bell,
  ChartColumn,
  CreditCard,
  HandCoins,
  Home,
  LayoutDashboard,
  Package,
  PiggyBank,
  Receipt,
  Users,
  Wallet,
  X,
} from 'lucide-react'
import { cn } from '@/lib/utils'

const navItems = [
  { to: '/dashboard', label: 'Tổng quan', icon: LayoutDashboard },
  { to: '/family', label: 'Gia đình', icon: Users },
  { to: '/wallets', label: 'Ví', icon: Wallet },
  { to: '/payments', label: 'Nạp tiền', icon: CreditCard },
  { to: '/budgets', label: 'Ngân sách', icon: PiggyBank },
  { to: '/allowances', label: 'Tiền tiêu vặt', icon: HandCoins },
  { to: '/expenses', label: 'Chi tiêu', icon: Receipt },
  { to: '/household', label: 'Gia dụng', icon: Package },
  { to: '/notifications', label: 'Thông báo', icon: Bell },
  { to: '/analytics', label: 'Phân tích', icon: ChartColumn },
]

interface SidebarProps {
  open: boolean
  onClose: () => void
}

export function Sidebar({ open, onClose }: SidebarProps) {
  return (
    <>
      <div
        className={cn('fixed inset-0 z-40 bg-ink/30 lg:hidden', open ? 'block' : 'hidden')}
        onClick={onClose}
      />
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-72 flex-col border-r border-line bg-white/95 backdrop-blur transition-transform lg:static lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex items-center justify-between border-b border-line px-5 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-700 text-white">
              <Home className="h-5 w-5" />
            </div>
            <div>
              <p className="font-display text-sm font-extrabold text-ink">Family Finance</p>
              <p className="text-xs text-muted">Quản lý tài chính gia đình</p>
            </div>
          </div>
          <button type="button" className="rounded-lg p-2 text-muted lg:hidden" onClick={onClose}>
            <X className="h-4 w-4" />
          </button>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onClose}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition',
                  isActive
                    ? 'bg-brand-50 text-brand-800'
                    : 'text-muted hover:bg-slate-50 hover:text-ink',
                )
              }
            >
              <item.icon className="h-4 w-4" />
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-line p-4 text-xs text-muted">
          Dữ liệu demo · Mock API · Không thanh toán thật
        </div>
      </aside>
    </>
  )
}
