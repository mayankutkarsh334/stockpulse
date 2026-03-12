import { useState } from 'react'
import { getPortfolio, PortfolioResponse } from '../api/portfolioApi'
import HoldingsTable from '../components/HoldingsTable'

export default function PortfolioPage() {
  const [portfolioId, setPortfolioId] = useState('')
  const [portfolio, setPortfolio] = useState<PortfolioResponse | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const load = async () => {
    if (!portfolioId.trim()) return
    setLoading(true)
    setError('')
    try {
      const data = await getPortfolio(portfolioId.trim())
      setPortfolio(data)
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  const pnlColor = (v: number) => v >= 0 ? 'text-green-600' : 'text-red-600'

  return (
    <div className="max-w-6xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">Portfolio Overview</h1>
      <div className="flex gap-3 mb-6">
        <input
          className="border rounded px-3 py-2 flex-1 max-w-sm"
          placeholder="Enter Portfolio ID"
          value={portfolioId}
          onChange={e => setPortfolioId(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && load()}
        />
        <button onClick={load} disabled={loading}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50">
          {loading ? 'Loading...' : 'Load'}
        </button>
      </div>
      {error && <div className="text-red-600 mb-4">{error}</div>}
      {portfolio && (
        <div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-white rounded-lg p-4 shadow">
              <div className="text-sm text-slate-500">Total Invested</div>
              <div className="text-xl font-bold">{portfolio.currency} {portfolio.totalInvested?.toFixed(2)}</div>
            </div>
            <div className="bg-white rounded-lg p-4 shadow">
              <div className="text-sm text-slate-500">Current Value</div>
              <div className="text-xl font-bold">{portfolio.currency} {portfolio.totalCurrentValue?.toFixed(2)}</div>
            </div>
            <div className="bg-white rounded-lg p-4 shadow">
              <div className="text-sm text-slate-500">Total P&amp;L</div>
              <div className={`text-xl font-bold ${pnlColor(portfolio.totalPnl)}`}>
                {portfolio.currency} {portfolio.totalPnl?.toFixed(2)}
              </div>
            </div>
            <div className="bg-white rounded-lg p-4 shadow">
              <div className="text-sm text-slate-500">Return %</div>
              <div className={`text-xl font-bold ${pnlColor(portfolio.totalPnlPercent)}`}>
                {portfolio.totalPnlPercent?.toFixed(2)}%
              </div>
            </div>
          </div>
          <HoldingsTable holdings={portfolio.holdings} currency={portfolio.currency} />
        </div>
      )}
    </div>
  )
}
