import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { Sidebar } from '@/components/layout/Sidebar'
import { Topbar } from '@/components/layout/Topbar'

export function AppShell() {
  const [open, setOpen] = useState(false)

  return (
    <div className="min-h-screen lg:flex">
      <Sidebar open={open} onClose={() => setOpen(false)} />
      <div className="flex min-h-screen min-w-0 flex-1 flex-col">
        <Topbar onMenuClick={() => setOpen(true)} />
        <main className="flex-1 px-4 py-6 md:px-6 lg:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
