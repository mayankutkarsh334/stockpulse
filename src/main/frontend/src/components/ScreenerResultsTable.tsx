import { ScreenerResult } from '../api/screenerApi'

interface Props { result: ScreenerResult }

export default function ScreenerResultsTable({ result }: Props) {
  return (
    <div className="bg-white rounded-lg shadow">
      <div className="p-4 border-b text-sm text-slate-600">
        Scanned {result.totalScanned} symbols — <strong>{result.totalMatched} matched</strong>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-slate-600">
            <tr>
              <th className="px-4 py-3 text-left">Symbol</th>
              <th className="px-4 py-3 text-left">Exchange</th>
              <th className="px-4 py-3 text-left">Passed Filters</th>
              <th className="px-4 py-3 text-left">Metrics</th>
            </tr>
          </thead>
          <tbody>
            {result.matches.map(m => (
              <tr key={`${m.symbol}-${m.exchange}`} className="border-t hover:bg-slate-50">
                <td className="px-4 py-3 font-medium text-blue-600">{m.symbol}</td>
                <td className="px-4 py-3">{m.exchange}</td>
                <td className="px-4 py-3">
                  {m.passedFilters.map(f => (
                    <span key={f} className="inline-block bg-green-100 text-green-700 text-xs px-2 py-0.5 rounded mr-1">{f}</span>
                  ))}
                </td>
                <td className="px-4 py-3 text-xs text-slate-500">
                  {Object.entries(m.metricValues).slice(0, 4).map(([k, v]) => `${k}: ${v?.toFixed(2)}`).join(' | ')}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
