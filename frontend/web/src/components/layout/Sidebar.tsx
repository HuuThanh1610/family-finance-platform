import { NavLink } from 'react-router-dom'
import {
  Bell,
  ChartColumn,
  CreditCard,
  HandCoins,
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
        className={cn('fixed inset-0 z-40 bg-fg/30 lg:hidden', open ? 'block' : 'hidden')}
        onClick={onClose}
      />
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-56 flex-col border-r border-border bg-bg transition-transform lg:static lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex h-12 items-center justify-between border-b border-border px-3">
          <div>
            <p className="text-[13px] font-semibold text-fg">Family Finance</p>
            <p className="text-[11px] text-fg-muted">Quản lý tài chính</p>
          </div>
          <button type="button" className="rounded-[2px] p-1.5 text-fg-secondary lg:hidden" onClick={onClose}>
            <X className="h-4 w-4" strokeWidth={1.75} />
          </button>
        </div>

        <nav className="flex-1 space-y-0.5 overflow-y-auto p-2">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onClose}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-2 rounded-[2px] px-2 py-1.5 text-[13px] transition-colors',
                  isActive
                    ? 'bg-bg-tertiary font-medium text-fg'
                    : 'text-fg-secondary hover:bg-bg-secondary hover:text-fg',
                )
              }
            >
              <item.icon className="h-4 w-4 shrink-0" strokeWidth={1.75} />
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="border-t border-border px-3 py-2 text-[11px] text-fg-muted">
          Demo · Mock API
        </div>
      </aside>
    </>
  )
}
