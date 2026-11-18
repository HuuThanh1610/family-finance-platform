import { cva, type VariantProps } from 'class-variance-authority'
import type { HTMLAttributes } from 'react'
import { cn } from '@/lib/utils'

const badgeVariants = cva(
  'inline-flex items-center rounded-[2px] border px-1.5 py-0.5 text-[11px] font-medium tracking-wide',
  {
    variants: {
      variant: {
        default: 'border-border bg-bg-tertiary text-fg',
        muted: 'border-border bg-bg text-fg-secondary',
        success: 'border-success/20 bg-success/5 text-success',
        warning: 'border-warning/20 bg-warning/5 text-warning',
        danger: 'border-danger/20 bg-danger/5 text-danger',
        info: 'border-border bg-bg-tertiary text-fg-secondary',
      },
    },
    defaultVariants: { variant: 'default' },
  },
)

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement>, VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />
}
