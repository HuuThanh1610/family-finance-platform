import type { InputHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        'h-10 w-full rounded-lg border border-line bg-white px-3 text-sm text-ink placeholder:text-muted/70 outline-none transition focus:border-brand-400 focus:ring-2 focus:ring-brand-100',
        className,
      )}
      {...props}
    />
  )
}
