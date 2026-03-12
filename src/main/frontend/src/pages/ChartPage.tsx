import { useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import StockChart from '../components/StockChart'

interface OhlcvPoint {
  date: string
  open: number
  high: number
  low: number
  close: number
  volume: number
}

export default function ChartPage() {
  const { symbol } = useParams<{ symbol: string }>()
  const [exchange, setExchange] = useState('NSE')
  const [data, setData] = useState<OhlcvPoint[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = async () => {
    if (!symbol) return
    setLoading(true)
    setError('')
    try {
      const res = await fetch(`/quotes/${symbol}/history?exchange=${exchange}`)
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      setData(await res.json())
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [symbol, exchange])

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center gap-4 mb-6">
        <h1 className="text-2xl font-bold">{symbol} Chart</h1>
        <select className="border rounded px-2 py-1"
          value={exchange} onChange={e => setExchange(e.target.value)}>
          <option>NSE</option><option>BSE</option><option>NYSE</option><option>NASDAQ</option>
        </select>
      </div>
      {loading && <div className="text-slate-500">Loading chart data...</div>}
      {error && <div className="text-red-600">{error}</div>}
      {data.length > 0 && <StockChart data={data} symbol={symbol ?? ''} />}
    </div>
  )
}
