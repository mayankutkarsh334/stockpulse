import { HoldingResponse } from '../api/portfolioApi'

interface Props {
  holdings: HoldingResponse[]
  currency: string
}

function formatDuration(days: number): string {
  const years = Math.floor(days / 365)
  const months = Math.floor((days % 365) / 30)
  return `${days}d (${years}y ${months}m)`
}

export default function CoffeeCanHoldingsTable({ holdings, currency }: Props) {
  const pnlColor = (v: number) => v >= 0 ? 'text-green-600' : 'text-red-600'

  return (
    <div className="bg-white rounded-lg shadow overflow-x-auto">
      <table className="w-full text-sm">
        <thead className="bg-slate-50 text-slate-600">
          <tr>
            {['Symbol', 'Exchange', 'Qty', 'Avg Buy', 'Current', 'Invested', 'Current Value', 'P&L', 'Return%', 'Hold Duration'].map(h => (
              <th key={h} className="px-4 py-3 text-left font-medium">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {holdings.map(h => (
            <tr key={h.id} className="border-t hover:bg-slate-50">
              <td className="px-4 py-3 font-medium text-blue-600">{h.symbol}</td>
              <td className="px-4 py-3">{h.exchange}</td>
              <td className="px-4 py-3">{h.quantity}</td>
              <td className="px-4 py-3">{h.averageBuyPrice?.toFixed(2)}</td>
              <td className="px-4 py-3">{h.currentPrice?.toFixed(2)}</td>
              <td className="px-4 py-3">{currency} {h.investedValue?.toFixed(2)}</td>
              <td className="px-4 py-3">{currency} {h.currentValue?.toFixed(2)}</td>
              <td className={`px-4 py-3 font-medium ${pnlColor(h.pnl)}`}>{currency} {h.pnl?.toFixed(2)}</td>
              <td className={`px-4 py-3 font-medium ${pnlColor(h.pnlPercent)}`}>{h.pnlPercent?.toFixed(2)}%</td>
              <td className="px-4 py-3 text-slate-500">
                {h.holdDurationDays != null ? formatDuration(h.holdDurationDays) : '—'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
