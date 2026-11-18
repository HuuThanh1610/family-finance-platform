import type { TextareaHTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

export function Textarea({ className, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      className={cn(
        'min-h-20 w-full rounded-[2px] border border-border bg-bg px-2.5 py-2 text-[13px] text-fg placeholder:text-fg-muted outline-none transition focus:border-border-strong focus:ring-1 focus:ring-fg/20',
        className,
      )}
      {...props}
    />
  )
}
