import type { SelectHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

export function Select({ className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={cn(
        'h-10 w-full rounded-lg border border-line bg-white px-3 text-sm text-ink outline-none transition focus:border-brand-400 focus:ring-2 focus:ring-brand-100',
        className,
      )}
      {...props}
    >
      {children}
    </select>
  )
}
