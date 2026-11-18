import { cva, type VariantProps } from 'class-variance-authority'
import type { HTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

const badgeVariants = cva('inline-flex items-center rounded-md px-2 py-0.5 text-xs font-semibold', {
  variants: {
    variant: {
      default: 'bg-brand-100 text-brand-800',
      muted: 'bg-slate-100 text-slate-700',
      success: 'bg-emerald-100 text-emerald-800',
      warning: 'bg-amber-100 text-amber-800',
      danger: 'bg-red-100 text-red-700',
      info: 'bg-sky-100 text-sky-800',
    },
  },
  defaultVariants: { variant: 'default' },
})

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement>, VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />
}
