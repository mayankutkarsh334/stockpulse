import { useState } from 'react'

interface Props {
  breakdown: Record<string, number>
  rawMetrics: Record<string, number | null>
}

type Tab = 'breakdown' | 'raw'

export default function MetricBreakdown({ breakdown, rawMetrics }: Props) {
  const [tab, setTab] = useState<Tab>('breakdown')

  const breakdownEntries = Object.entries(breakdown)
  const rawEntries = Object.entries(rawMetrics).filter(([, v]) => v != null)

  return (
    <div className="mt-4 pt-4 border-t">
      <div className="flex border-b mb-3">
        <button
          className={`px-4 py-1.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
            tab === 'breakdown'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          }`}
          onClick={() => setTab('breakdown')}
        >
          Score Breakdown
        </button>
        <button
          className={`px-4 py-1.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
            tab === 'raw'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-700'
          }`}
          onClick={() => setTab('raw')}
        >
          Raw Metrics
        </button>
      </div>

      {tab === 'breakdown' && (
        breakdownEntries.length > 0 ? (
          <div className="space-y-0.5">
            {breakdownEntries.map(([k, v]) => (
              <div key={k} className="flex justify-between text-sm py-0.5">
                <span className="text-slate-600">{k}</span>
                <span className="font-medium">{v?.toFixed(4)}</span>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-slate-400 italic">No breakdown data available.</p>
        )
      )}

      {tab === 'raw' && (
        rawEntries.length > 0 ? (
          <div className="space-y-0.5">
            {rawEntries.map(([k, v]) => (
              <div key={k} className="flex justify-between text-sm py-0.5">
                <span className="text-slate-600">{k}</span>
                <span className="font-medium">{typeof v === 'number' ? v.toFixed(4) : v}</span>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-slate-400 italic">No raw metrics available.</p>
        )
      )}
    </div>
  )
}
