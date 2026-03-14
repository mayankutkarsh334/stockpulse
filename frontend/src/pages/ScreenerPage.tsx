import { useState } from 'react'
import { runScan, FilterCriteria, ScreenerResult } from '../api/screenerApi'
import FilterBuilder from '../components/FilterBuilder'
import ScreenerResultsTable from '../components/ScreenerResultsTable'
import StockSymbolPicker from '../components/StockSymbolPicker'

const INDICATORS = ['PE', 'EPS', 'EPS_GROWTH', 'RSI_14', 'MACD_VALUE', 'SMA_50', 'MARKET_CAP',
  'DEBT_TO_EQUITY', 'CURRENT_RATIO', 'ROA', 'DIVIDEND_YIELD']

export default function ScreenerPage() {
  const [symbols, setSymbols] = useState(['RELIANCE', 'TCS', 'INFY'])
  const [exchange, setExchange] = useState('NSE')
  const [filters, setFilters] = useState<FilterCriteria[]>([
    { indicator: 'RSI_14', operator: 'LT', value: 50 }
  ])
  const [result, setResult] = useState<ScreenerResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleScan = async () => {
    setLoading(true)
    setError('')
    try {
      const data = await runScan(symbols, exchange, filters)
      setResult(data)
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">Stock Screener</h1>
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
          <div>
            <label className="block text-sm font-medium mb-1">Symbols</label>
            <StockSymbolPicker exchange={exchange} selected={symbols} onChange={setSymbols} />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Exchange</label>
            <select className="border rounded px-3 py-2 w-full"
              value={exchange} onChange={e => setExchange(e.target.value)}>
              <option>NSE</option><option>BSE</option><option>NYSE</option><option>NASDAQ</option>
            </select>
          </div>
        </div>
        <FilterBuilder filters={filters} onChange={setFilters} indicators={INDICATORS} />
        <button onClick={handleScan} disabled={loading}
          className="mt-4 bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50">
          {loading ? 'Scanning...' : 'Run Scan'}
        </button>
      </div>
      {error && <div className="text-red-600 mb-4">{error}</div>}
      {result && <ScreenerResultsTable result={result} />}
    </div>
  )
}
