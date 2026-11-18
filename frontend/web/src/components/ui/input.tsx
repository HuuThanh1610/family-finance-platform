import type { InputHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        'h-8 w-full rounded-[2px] border border-border bg-bg px-2.5 text-[13px] text-fg placeholder:text-fg-muted outline-none transition focus:border-border-strong focus:ring-1 focus:ring-fg/20',
        className,
      )}
      {...props}
    />
  )
}
