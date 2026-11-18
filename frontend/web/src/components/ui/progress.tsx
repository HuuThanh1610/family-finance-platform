import { cn } from '@/lib/utils'

interface ProgressProps {
  value: number
  max?: number
  className?: string
  barClassName?: string
}

export function Progress({ value, max = 100, className, barClassName }: ProgressProps) {
  const pct = Math.min(100, Math.max(0, (value / max) * 100))
  const over = value > max

  return (
    <div className={cn('h-1.5 w-full overflow-hidden rounded-[2px] bg-bg-tertiary', className)}>
      <div
        className={cn('h-full rounded-[2px] transition-all', over ? 'bg-danger' : 'bg-fg', barClassName)}
        style={{ width: `${pct}%` }}
      />
    </div>
  )
}
