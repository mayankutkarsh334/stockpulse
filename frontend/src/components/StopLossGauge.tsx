import { Position } from '../api/strategyApi'

interface Props {
  position: Position
}

export default function StopLossGauge({ position: p }: Props) {
  const entry    = p.entryPrice
  const stop     = p.currentStopLoss
  const hwm      = p.highWaterMarkPrice
  const target30 = entry * 1.30

  // Bar spans from just below stop to just above max(hwm, target30)
  const barMin = stop * 0.99
  const barMax = Math.max(hwm, target30) * 1.02
  const range  = barMax - barMin

  const pct = (val: number) =>
    `${Math.max(0, Math.min(100, ((val - barMin) / range) * 100)).toFixed(1)}%`

  return (
    <div className="mt-4">
      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-1">
        Stop-Loss Gauge
      </p>
      <div className="relative h-5 bg-slate-100 rounded-full border border-slate-200 overflow-visible">
        {/* Green fill to HWM */}
        <div
          className="absolute top-0 left-0 h-full bg-green-200 rounded-full"
          style={{ width: pct(hwm) }}
        />
        {/* Entry marker */}
        <div
          className="absolute top-0 h-full w-0.5 bg-blue-500"
          style={{ left: pct(entry) }}
          title={`Entry ₹${entry.toFixed(2)}`}
        />
        {/* Stop marker */}
        <div
          className="absolute top-0 h-full w-1 bg-red-500 rounded"
          style={{ left: pct(stop) }}
          title={`Stop ₹${stop.toFixed(2)}`}
        />
        {/* +30% target — dashed line */}
        <div
          className="absolute top-0 h-full border-l-2 border-dashed border-yellow-500"
          style={{ left: pct(target30) }}
          title={`+30% Target ₹${target30.toFixed(2)}`}
        />
        {/* HWM dot */}
        <div
          className="absolute top-1/2 -translate-y-1/2 w-3 h-3 bg-green-500 rounded-full border-2 border-white shadow"
          style={{ left: `calc(${pct(hwm)} - 6px)` }}
          title={`HWM ₹${hwm.toFixed(2)}`}
        />
      </div>
      <div className="flex justify-between text-xs mt-1">
        <span className="text-red-500">Stop ₹{stop.toFixed(2)}</span>
        <span className="text-blue-500">Entry ₹{entry.toFixed(2)}</span>
        <span className="text-green-600">HWM ₹{hwm.toFixed(2)}</span>
        <span className="text-yellow-600">+30% ₹{target30.toFixed(2)}</span>
      </div>
    </div>
  )
}
