interface Props {
  breakdown: Record<string, number>
  rawMetrics: Record<string, number | null>
}

export default function MetricBreakdown({ breakdown, rawMetrics }: Props) {
  return (
    <div className="mt-4 pt-4 border-t grid grid-cols-1 md:grid-cols-2 gap-4">
      <div>
        <div className="text-xs font-semibold text-slate-500 mb-2">Score Breakdown</div>
        {Object.entries(breakdown).map(([k, v]) => (
          <div key={k} className="flex justify-between text-sm py-0.5">
            <span className="text-slate-600">{k}</span>
            <span className="font-medium">{v?.toFixed(4)}</span>
          </div>
        ))}
      </div>
      <div>
        <div className="text-xs font-semibold text-slate-500 mb-2">Raw Metrics</div>
        {Object.entries(rawMetrics).filter(([, v]) => v != null).map(([k, v]) => (
          <div key={k} className="flex justify-between text-sm py-0.5">
            <span className="text-slate-600">{k}</span>
            <span className="font-medium">{typeof v === 'number' ? v.toFixed(4) : v}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
