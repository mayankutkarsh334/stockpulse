import { HoldingResponse } from '../api/portfolioApi'
import { useNavigate } from 'react-router-dom'

interface Props {
  holdings: HoldingResponse[]
  currency: string
}

export default function HoldingsTable({ holdings, currency }: Props) {
  const navigate = useNavigate()
  const pnlColor = (v: number) => v >= 0 ? 'text-green-600' : 'text-red-600'

  return (
    <div className="bg-white rounded-lg shadow overflow-x-auto">
      <table className="w-full text-sm">
        <thead className="bg-slate-50 text-slate-600">
          <tr>
            {['Symbol', 'Exchange', 'Qty', 'Avg Buy', 'Current', 'Invested', 'Current Value', 'P&L', 'Return%'].map(h => (
              <th key={h} className="px-4 py-3 text-left font-medium">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {holdings.map(h => (
            <tr key={h.id} className="border-t hover:bg-slate-50 cursor-pointer"
              onClick={() => navigate(`/chart/${h.symbol}?exchange=${h.exchange}`)}>
              <td className="px-4 py-3 font-medium text-blue-600">{h.symbol}</td>
              <td className="px-4 py-3">{h.exchange}</td>
              <td className="px-4 py-3">{h.quantity}</td>
              <td className="px-4 py-3">{h.averageBuyPrice?.toFixed(2)}</td>
              <td className="px-4 py-3">{h.currentPrice?.toFixed(2)}</td>
              <td className="px-4 py-3">{currency} {h.investedValue?.toFixed(2)}</td>
              <td className="px-4 py-3">{currency} {h.currentValue?.toFixed(2)}</td>
              <td className={`px-4 py-3 font-medium ${pnlColor(h.pnl)}`}>{currency} {h.pnl?.toFixed(2)}</td>
              <td className={`px-4 py-3 font-medium ${pnlColor(h.pnlPercent)}`}>{h.pnlPercent?.toFixed(2)}%</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
