export function LoadingState({ label = 'Đang tải dữ liệu...' }: { label?: string }) {
  return (
    <div className="flex min-h-40 flex-col items-center justify-center gap-3 rounded-2xl border border-line bg-white/70 p-6">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-brand-200 border-t-brand-700" />
      <p className="text-sm text-muted">{label}</p>
    </div>
  )
}
