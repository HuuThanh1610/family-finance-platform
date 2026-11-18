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
    <div className="flex min-h-40 flex-col items-center justify-center gap-3 rounded-2xl border border-red-200 bg-red-50/60 p-6 text-center">
      <AlertTriangle className="h-6 w-6 text-danger" />
      <p className="text-sm text-danger">{message}</p>
      {onRetry ? (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Thử lại
        </Button>
      ) : null}
    </div>
  )
}
