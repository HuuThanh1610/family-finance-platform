import { AlertTriangle } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface ErrorStateProps {
  message?: string
  onRetry?: () => void
}

export function ErrorState({
  message = 'Không thể tải dữ liệu. Vui lòng thử lại.',
  onRetry,
}: ErrorStateProps) {
  return (
    <div className="flex min-h-32 flex-col items-center justify-center gap-2 rounded-[2px] border border-danger/20 bg-bg p-6 text-center">
      <AlertTriangle className="h-4 w-4 text-danger" strokeWidth={1.75} />
      <p className="text-[13px] text-danger">{message}</p>
      {onRetry ? (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Thử lại
        </Button>
      ) : null}
    </div>
  )
}
