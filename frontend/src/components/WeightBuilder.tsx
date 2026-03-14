import { useState } from 'react'

const ALL_METRICS = [
  'PE','EPS','EPS_GROWTH','REVENUE_GROWTH','MARKET_CAP','DIVIDEND_YIELD',
  'DEBT_TO_EQUITY','CURRENT_RATIO','ROA','GROSS_MARGIN','ASSET_TURNOVER',
  'RETAINED_EARNINGS','EBIT','WORKING_CAPITAL','TOTAL_ASSETS','TOTAL_LIABILITIES',
  'OPERATING_CASH_FLOW','SHARES_OUTSTANDING','HIGH_52W','LOW_52W',
  'RSI_14','MACD_VALUE','MACD_SIGNAL','MACD_HISTOGRAM',
  'SMA_20','SMA_50','SMA_200','CURRENT_PRICE','VOLUME','AVG_VOLUME',
]

const PRESETS: Record<string, Record<string, number>> = {
  Growth: { EPS_GROWTH: 0.40, REVENUE_GROWTH: 0.30, ROA: 0.20, RSI_14: 0.10 },
  Value: { PE: 0.35, CURRENT_RATIO: 0.25, DEBT_TO_EQUITY: 0.25, DIVIDEND_YIELD: 0.15 },
  Quality: { ROA: 0.30, GROSS_MARGIN: 0.25, CURRENT_RATIO: 0.25, ASSET_TURNOVER: 0.20 },
}

interface Props {
  weights: Record<string, number>
  onChange: (weights: Record<string, number>) => void
}

export default function WeightBuilder({ weights, onChange }: Props) {
  const [selectedMetric, setSelectedMetric] = useState('')

  const total = Object.values(weights).reduce((s, v) => s + v, 0)

  const totalColor =
    total >= 0.99 && total <= 1.01
      ? 'text-green-600'
      : total >= 0.8 && total <= 1.2
      ? 'text-yellow-600'
      : 'text-red-600'

  const updateWeight = (metric: string, value: number) => {
    onChange({ ...weights, [metric]: Math.max(0, Math.min(1, value)) })
  }

  const removeMetric = (metric: string) => {
    const next = { ...weights }
    delete next[metric]
    onChange(next)
  }

  const addMetric = (metric: string) => {
    if (!metric || metric in weights) return
    onChange({ ...weights, [metric]: 0.10 })
    setSelectedMetric('')
  }

  const normalize = () => {
    if (total === 0) return
    const next: Record<string, number> = {}
    for (const [k, v] of Object.entries(weights)) {
      next[k] = Math.round((v / total) * 100) / 100
    }
    onChange(next)
  }

  const available = ALL_METRICS.filter(m => !(m in weights))

  return (
    <div className="border rounded p-4 space-y-3">
      {/* Preset buttons */}
      <div className="flex gap-2 flex-wrap">
        <span className="text-sm font-medium self-center">Presets:</span>
        {Object.keys(PRESETS).map(name => (
          <button
            key={name}
            type="button"
            onClick={() => onChange(PRESETS[name])}
            className="text-sm px-3 py-1 rounded bg-gray-100 hover:bg-gray-200 border"
          >
            {name}
          </button>
        ))}
      </div>

      {/* Metric rows */}
      {Object.entries(weights).map(([metric, value]) => (
        <div key={metric} className="flex items-center gap-2">
          <span className="w-36 text-sm font-mono truncate">{metric}</span>
          <input
            type="range"
            min={0} max={1} step={0.01}
            value={value}
            onChange={e => updateWeight(metric, parseFloat(e.target.value))}
            className="flex-1"
          />
          <input
            type="number"
            min={0} max={1} step={0.01}
            value={value}
            onChange={e => updateWeight(metric, parseFloat(e.target.value) || 0)}
            className="w-20 border rounded px-2 py-1 text-sm text-right"
          />
          <button
            type="button"
            onClick={() => removeMetric(metric)}
            className="text-red-500 hover:text-red-700 font-bold px-1"
            title="Remove"
          >
            ×
          </button>
        </div>
      ))}

      {/* Add metric */}
      <div className="flex items-center gap-2">
        <select
          value={selectedMetric}
          onChange={e => addMetric(e.target.value)}
          className="border rounded px-2 py-1 text-sm flex-1"
        >
          <option value="">+ Add metric…</option>
          {available.map(m => <option key={m} value={m}>{m}</option>)}
        </select>
      </div>

      {/* Total + normalize */}
      <div className="flex items-center gap-4 pt-1">
        <span className={`text-sm font-semibold ${totalColor}`}>
          Total: {total.toFixed(2)}
        </span>
        <button
          type="button"
          onClick={normalize}
          className="text-sm px-3 py-1 rounded bg-blue-50 hover:bg-blue-100 border border-blue-200 text-blue-700"
        >
          Normalize to 1.0
        </button>
      </div>
    </div>
  )
}
