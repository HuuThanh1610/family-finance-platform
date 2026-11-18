import type { ReactNode } from 'react'
import { X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

interface DialogProps {
  open: boolean
  title: string
  description?: string
  onClose: () => void
  children: ReactNode
  className?: string
}

export function Dialog({ open, title, description, onClose, children, className }: DialogProps) {
  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button
        type="button"
        aria-label="Đóng hộp thoại"
        className="absolute inset-0 bg-fg/40"
        onClick={onClose}
      />
      <div
        role="dialog"
        aria-modal="true"
        className={cn(
          'relative z-10 w-full max-w-lg rounded-[4px] border border-border bg-bg p-4 shadow-sm',
          className,
        )}
      >
        <div className="mb-3 flex items-start justify-between gap-3">
          <div>
            <h2 className="text-[16px] font-semibold text-fg">{title}</h2>
            {description ? <p className="mt-1 text-[13px] text-fg-secondary">{description}</p> : null}
          </div>
          <Button variant="ghost" size="sm" onClick={onClose} aria-label="Đóng">
            <X className="h-4 w-4" strokeWidth={1.75} />
          </Button>
        </div>
        {children}
      </div>
    </div>
  )
}
