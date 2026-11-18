export function LoadingState({ label = 'Đang tải...' }: { label?: string }) {
  return (
    <div className="flex min-h-32 flex-col items-center justify-center gap-2 rounded-[2px] border border-border bg-bg p-6">
      <div className="h-4 w-4 animate-spin rounded-full border border-border border-t-fg" />
      <p className="text-[13px] text-fg-secondary">{label}</p>
    </div>
  )
}
