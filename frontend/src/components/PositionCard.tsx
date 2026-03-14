import { Position } from '../api/strategyApi'
import StopLossGauge from './StopLossGauge'

interface Props {
  position: Position
}

const STATUS_BADGE: Record<string, string> = {
  ACTIVE:          'bg-green-100 text-green-800',
  STOPPED_OUT:     'bg-red-100 text-red-800',
  TARGET_MISSED:   'bg-orange-100 text-orange-800',
  MANUALLY_CLOSED: 'bg-slate-100 text-slate-700',
}

export default function PositionCard({ position: p }: Props) {
  const gainColor  = p.currentGainPct >= 0 ? 'text-green-600' : 'text-red-600'
  const badgeStyle = STATUS_BADGE[p.status] ?? 'bg-slate-100 text-slate-700'

  return (
    <div className="bg-white rounded-xl shadow-md border border-slate-200 p-6">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-0.5">
            {p.modelType.replace(/_/g, ' ')} · {p.pickedMonth}
          </p>
          <h2 className="text-xl font-bold text-slate-800">{p.companyName}</h2>
          <p className="text-slate-500 text-sm font-mono">{p.symbol} · {p.exchange}</p>
        </div>
        <div className="flex flex-col items-end gap-1">
          <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${badgeStyle}`}>
            {p.status.replace(/_/g, ' ')}
          </span>
          <span className={`text-xl font-bold ${gainColor}`}>
            {p.currentGainPct >= 0 ? '+' : ''}{p.currentGainPct.toFixed(2)}%
          </span>
          <span className="text-xs text-slate-400">HWM gain</span>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-4 mt-4 text-sm">
        <div>
          <p className="text-slate-400 text-xs">Entry Price</p>
          <p className="font-semibold text-slate-700">₹{p.entryPrice.toFixed(2)}</p>
        </div>
        <div>
          <p className="text-slate-400 text-xs">Entry Date</p>
          <p className="font-semibold text-slate-700">{p.entryDate}</p>
        </div>
        <div>
          <p className="text-slate-400 text-xs">Current Stop</p>
          <p className="font-semibold text-red-600">₹{p.currentStopLoss.toFixed(2)}</p>
        </div>
      </div>

      <StopLossGauge position={p} />
    </div>
  )
}
