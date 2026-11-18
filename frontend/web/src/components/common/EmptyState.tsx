import type { ReactNode } from 'react'

interface EmptyStateProps {
  title: string
  description?: string
  action?: ReactNode
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center rounded-[2px] border border-dashed border-border bg-bg px-6 py-10 text-center">
      <h3 className="text-[14px] font-medium text-fg">{title}</h3>
      {description ? <p className="mt-1 max-w-sm text-[13px] text-fg-secondary">{description}</p> : null}
      {action ? <div className="mt-3">{action}</div> : null}
    </div>
  )
}
