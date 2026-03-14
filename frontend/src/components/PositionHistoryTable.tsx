import { Position } from '../api/strategyApi'

interface Props {
  positions: Position[]
}

const STATUS_BADGE: Record<string, string> = {
  ACTIVE:          'bg-green-100 text-green-800',
  STOPPED_OUT:     'bg-red-100 text-red-800',
  TARGET_MISSED:   'bg-orange-100 text-orange-800',
  MANUALLY_CLOSED: 'bg-slate-100 text-slate-700',
}

export default function PositionHistoryTable({ positions }: Props) {
  if (positions.length === 0) {
    return <p className="text-slate-400 text-sm text-center py-8">No closed positions yet.</p>
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-xs text-slate-500 uppercase border-b border-slate-200">
            <th className="pb-2 pr-4">Month</th>
            <th className="pb-2 pr-4">Company</th>
            <th className="pb-2 pr-4">Model</th>
            <th className="pb-2 pr-4 text-right">Entry ₹</th>
            <th className="pb-2 pr-4 text-right">Close ₹</th>
            <th className="pb-2 pr-4 text-right">P&amp;L ₹</th>
            <th className="pb-2 pr-4">Reason</th>
            <th className="pb-2">Status</th>
          </tr>
        </thead>
        <tbody>
          {positions.map(p => {
            const pnlColor   = p.closePnl != null && p.closePnl >= 0 ? 'text-green-600' : 'text-red-600'
            const badgeStyle = STATUS_BADGE[p.status] ?? 'bg-slate-100 text-slate-700'
            return (
              <tr key={p.id} className="border-b border-slate-100 hover:bg-slate-50">
                <td className="py-3 pr-4 font-mono text-slate-600">{p.pickedMonth}</td>
                <td className="py-3 pr-4">
                  <p className="font-medium text-slate-800">{p.companyName}</p>
                  <p className="text-xs text-slate-400 font-mono">{p.symbol}</p>
                </td>
                <td className="py-3 pr-4 text-slate-500">{p.modelType.replace(/_/g, ' ')}</td>
                <td className="py-3 pr-4 text-right font-mono">₹{p.entryPrice.toFixed(2)}</td>
                <td className="py-3 pr-4 text-right font-mono">
                  {p.closePrice != null ? `₹${p.closePrice.toFixed(2)}` : '—'}
                </td>
                <td className={`py-3 pr-4 text-right font-mono font-semibold ${pnlColor}`}>
                  {p.closePnl != null
                    ? (p.closePnl >= 0 ? '+' : '') + `₹${p.closePnl.toFixed(2)}`
                    : '—'}
                </td>
                <td className="py-3 pr-4 text-slate-500">
                  {p.closeReason?.replace(/_/g, ' ') ?? '—'}
                </td>
                <td className="py-3">
                  <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${badgeStyle}`}>
                    {p.status.replace(/_/g, ' ')}
                  </span>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
