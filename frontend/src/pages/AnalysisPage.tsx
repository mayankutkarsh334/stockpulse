import { useState } from 'react'
import { runAnalysis, AnalysisResult } from '../api/analysisApi'
import RankedStocksList from '../components/RankedStocksList'

const MODELS = ['WEIGHTED_SCORE', 'PIOTROSKI', 'ALTMAN_Z', 'RELATIVE', 'CUSTOM']

export default function AnalysisPage() {
  const [symbolsText, setSymbolsText] = useState('RELIANCE,TCS,INFY,WIPRO,HCLTECH')
  const [exchange, setExchange] = useState('NSE')
  const [model, setModel] = useState('WEIGHTED_SCORE')
  const [formula, setFormula] = useState('EPS_GROWTH / PE')
  const [weightsText, setWeightsText] = useState('{"PE": 0.3, "EPS_GROWTH": 0.4, "RSI_14": 0.3}')
  const [result, setResult] = useState<AnalysisResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleRun = async () => {
    const symbols = symbolsText.split(',').map(s => s.trim()).filter(Boolean)
    let params: Record<string, unknown> = {}
    if (model === 'WEIGHTED_SCORE') {
      try { params = { weights: JSON.parse(weightsText) } } catch { setError('Invalid weights JSON'); return }
    } else if (model === 'CUSTOM') {
      params = { formula }
    }
    setLoading(true)
    setError('')
    try {
      const data = await runAnalysis(symbols, exchange, model, params)
      setResult(data)
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto">
      <h1 className="text-2xl font-bold mb-6">Stock Analysis</h1>
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
          <div>
            <label className="block text-sm font-medium mb-1">Symbols</label>
            <input className="border rounded px-3 py-2 w-full"
              value={symbolsText} onChange={e => setSymbolsText(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Exchange</label>
            <select className="border rounded px-3 py-2 w-full"
              value={exchange} onChange={e => setExchange(e.target.value)}>
              <option>NSE</option><option>BSE</option><option>NYSE</option><option>NASDAQ</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Model</label>
            <select className="border rounded px-3 py-2 w-full"
              value={model} onChange={e => setModel(e.target.value)}>
              {MODELS.map(m => <option key={m}>{m}</option>)}
            </select>
          </div>
        </div>
        {model === 'WEIGHTED_SCORE' && (
          <div className="mb-4">
            <label className="block text-sm font-medium mb-1">Weights (JSON)</label>
            <textarea className="border rounded px-3 py-2 w-full font-mono text-sm"
              rows={3} value={weightsText} onChange={e => setWeightsText(e.target.value)} />
          </div>
        )}
        {model === 'CUSTOM' && (
          <div className="mb-4">
            <label className="block text-sm font-medium mb-1">Custom Formula</label>
            <input className="border rounded px-3 py-2 w-full font-mono"
              value={formula} onChange={e => setFormula(e.target.value)}
              placeholder="e.g. (EPS_GROWTH / PE) * (1 / RSI_14)" />
          </div>
        )}
        <button onClick={handleRun} disabled={loading}
          className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50">
          {loading ? 'Analyzing...' : 'Run Analysis'}
        </button>
      </div>
      {error && <div className="text-red-600 mb-4">{error}</div>}
      {result && <RankedStocksList result={result} />}
    </div>
  )
}
