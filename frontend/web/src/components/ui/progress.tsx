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
    <div className={cn('h-2 w-full overflow-hidden rounded-full bg-slate-100', className)}>
      <div
        className={cn('h-full rounded-full transition-all', over ? 'bg-danger' : 'bg-brand-600', barClassName)}
        style={{ width: `${pct}%` }}
      />
    </div>
  )
}
