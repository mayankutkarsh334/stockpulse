import { CoffeeCanStock } from '../api/coffeeCanApi'

interface Props {
  stocks: CoffeeCanStock[]
}

function fmt(v: number | null): string {
  return v != null ? v.toFixed(2) : 'N/A'
}

const TIER_ORDER: Record<string, number> = { TIER_1: 0, TIER_2: 1, FAIL: 2 }

function TierBadge({ stock }: { stock: CoffeeCanStock }) {
  const tooltip = stock.failReasons.length > 0 ? stock.failReasons.join(', ') : undefined
  if (stock.tier === 'TIER_1') {
    return (
      <span className="px-2 py-1 rounded text-xs font-semibold bg-green-100 text-green-800">
        TIER 1
      </span>
    )
  }
  if (stock.tier === 'TIER_2') {
    return (
      <span
        className="px-2 py-1 rounded text-xs font-semibold bg-amber-100 text-amber-800 cursor-help"
        title={tooltip}
      >
        TIER 2
      </span>
    )
  }
  return (
    <span
      className="px-2 py-1 rounded text-xs font-semibold bg-red-100 text-red-700 cursor-help"
      title={tooltip}
    >
      FAIL
    </span>
  )
}

export default function CoffeeCanScreenResults({ stocks }: Props) {
  const sorted = [...stocks].sort((a, b) => {
    const tierDiff = (TIER_ORDER[a.tier] ?? 3) - (TIER_ORDER[b.tier] ?? 3)
    if (tierDiff !== 0) return tierDiff
    return (b.roce ?? -Infinity) - (a.roce ?? -Infinity)
  })

  return (
    <div className="bg-white rounded-lg shadow overflow-x-auto">
      <table className="w-full text-sm">
        <thead className="bg-slate-50 text-slate-600">
          <tr>
            {['Symbol', 'Company', 'ROCE (%)', 'Rev Growth (%)', 'D/E', 'Pledged (%)', 'Mkt Cap (Cr)', 'PB', 'Tier'].map(h => (
              <th key={h} className="px-4 py-3 text-left font-medium">{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {sorted.map(s => (
            <tr key={s.symbol} className="border-t hover:bg-slate-50">
              <td className="px-4 py-3 font-medium">{s.symbol}</td>
              <td className="px-4 py-3">{s.companyName}</td>
              <td className="px-4 py-3">{fmt(s.roce)}</td>
              <td className="px-4 py-3">{fmt(s.revenueGrowth)}</td>
              <td className="px-4 py-3">{fmt(s.debtToEquity)}</td>
              <td className="px-4 py-3">{s.pledgedPromoterHoldings != null ? fmt(s.pledgedPromoterHoldings) : 'N/A'}</td>
              <td className="px-4 py-3">{s.marketCap != null ? s.marketCap.toFixed(0) : 'N/A'}</td>
              <td className="px-4 py-3">{fmt(s.pbRatio)}</td>
              <td className="px-4 py-3">
                <TierBadge stock={s} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
