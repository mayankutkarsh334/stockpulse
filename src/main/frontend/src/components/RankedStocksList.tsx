import { useState } from 'react'
import { AnalysisResult } from '../api/analysisApi'
import MetricBreakdown from './MetricBreakdown'

interface Props { result: AnalysisResult }

export default function RankedStocksList({ result }: Props) {
  const [expanded, setExpanded] = useState<string | null>(null)
  const rankColor = (r: number) => r === 1 ? 'bg-yellow-400' : r === 2 ? 'bg-slate-300' : r === 3 ? 'bg-amber-600 text-white' : 'bg-slate-100'

  return (
    <div className="space-y-3">
      <div className="text-sm text-slate-500 mb-4">Model: {result.modelType} — {result.rankings.length} stocks ranked</div>
      {result.rankings.map(stock => (
        <div key={stock.symbol} className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${rankColor(stock.rank)}`}>
                #{stock.rank}
              </span>
              <div>
                <div className="font-semibold text-lg">{stock.symbol}</div>
                <div className="text-xs text-slate-500">{stock.exchange}</div>
              </div>
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-blue-600">{stock.score.toFixed(3)}</div>
              <button className="text-xs text-slate-500 hover:text-blue-600 mt-1"
                onClick={() => setExpanded(expanded === stock.symbol ? null : stock.symbol)}>
                {expanded === stock.symbol ? 'Hide' : 'Show'} breakdown
              </button>
            </div>
          </div>
          {expanded === stock.symbol && (
            <MetricBreakdown breakdown={stock.breakdown} rawMetrics={stock.rawMetrics} />
          )}
        </div>
      ))}
    </div>
  )
}
