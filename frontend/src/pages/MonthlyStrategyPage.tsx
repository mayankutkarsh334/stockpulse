import { useState, useEffect, useRef } from 'react'
import { getCurrentPosition, listPositions, createMonthlyPick, Position } from '../api/strategyApi'
import { runCsvAnalysis, CsvAnalysisResult } from '../api/csvAnalysisApi'
import PositionCard from '../components/PositionCard'
import InvestmentRecommendationCard from '../components/InvestmentRecommendationCard'
import PositionHistoryTable from '../components/PositionHistoryTable'

const MODEL_OPTIONS = [
  'MAGIC_FORMULA', 'MULTI_FACTOR', 'WEIGHTED_SCORE', 'PIOTROSKI', 'ALTMAN_Z', 'RELATIVE', 'CUSTOM',
]

export default function MonthlyStrategyPage() {
  const [currentPosition, setCurrentPosition] = useState<Position | null>(null)
  const [history, setHistory]                 = useState<Position[]>([])
  const [loading, setLoading]                 = useState(true)
  const [selectedModel, setSelectedModel]     = useState('MAGIC_FORMULA')
  const [csvResult, setCsvResult]             = useState<CsvAnalysisResult | null>(null)
  const [csvLoading, setCsvLoading]           = useState(false)
  const [csvError, setCsvError]               = useState<string | null>(null)
  const [confirmLoading, setConfirmLoading]   = useState(false)
  const [confirmError, setConfirmError]       = useState<string | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)

  const loadData = async () => {
    setLoading(true)
    try {
      const [current, all] = await Promise.all([getCurrentPosition(), listPositions()])
      setCurrentPosition(current)
      setHistory(all.filter(p => p.status !== 'ACTIVE'))
    } catch {
      // silently ignore load errors
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadData() }, [])

  const handleCsvUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setCsvError(null)
    setCsvLoading(true)
    setCsvResult(null)
    try {
      const content = await file.text()
      const result  = await runCsvAnalysis({ csvContent: content, modelType: selectedModel })
      setCsvResult(result)
    } catch (err: any) {
      setCsvError(err.message ?? 'Analysis failed')
    } finally {
      setCsvLoading(false)
    }
  }

  const handleConfirmPick = async () => {
    if (!csvResult?.recommendation) return
    const rec = csvResult.recommendation
    if (!rec.closePrice) {
      setConfirmError('Close price is not available in the CSV — cannot invest.')
      return
    }
    setConfirmError(null)
    setConfirmLoading(true)
    try {
      await createMonthlyPick({
        symbol:      rec.symbol,
        companyName: rec.companyName,
        exchange:    'NSE',
        entryPrice:  rec.closePrice,
        modelType:   selectedModel,
      })
      await loadData()
      setCsvResult(null)
      if (fileRef.current) fileRef.current.value = ''
    } catch (err: any) {
      setConfirmError(err.message ?? 'Failed to create pick')
    } finally {
      setConfirmLoading(false)
    }
  }

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <h1 className="text-2xl font-bold text-slate-800">Monthly Strategy</h1>

      {/* ── Section 1: Current Position ─────────────────────────────────── */}
      <section>
        <h2 className="text-lg font-semibold text-slate-700 mb-3">Current Month Position</h2>
        {loading ? (
          <p className="text-slate-400 text-sm">Loading…</p>
        ) : currentPosition ? (
          <PositionCard position={currentPosition} />
        ) : (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-blue-700 text-sm">
            No pick this month yet — run an analysis below to select your monthly stock.
          </div>
        )}
      </section>

      {/* ── Section 2: Create Monthly Pick (only when no active position) ── */}
      {!loading && !currentPosition && (
        <section>
          <h2 className="text-lg font-semibold text-slate-700 mb-3">Create Monthly Pick</h2>
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 space-y-4">
            <div className="flex items-center gap-4 flex-wrap">
              <div className="flex items-center gap-2">
                <label className="text-sm font-medium text-slate-600">Model</label>
                <select
                  className="border border-slate-300 rounded-lg px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-400"
                  value={selectedModel}
                  onChange={e => { setSelectedModel(e.target.value); setCsvResult(null) }}
                >
                  {MODEL_OPTIONS.map(m => (
                    <option key={m} value={m}>{m.replace(/_/g, ' ')}</option>
                  ))}
                </select>
              </div>

              <div className="flex items-center gap-2">
                <label className="text-sm font-medium text-slate-600">Upload CSV</label>
                <input
                  ref={fileRef}
                  type="file"
                  accept=".csv"
                  className="text-sm text-slate-600 file:mr-3 file:py-1.5 file:px-4 file:rounded-lg file:border-0 file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 cursor-pointer"
                  onChange={handleCsvUpload}
                />
              </div>
            </div>

            {csvLoading && <p className="text-slate-400 text-sm">Analysing CSV…</p>}
            {csvError   && <p className="text-red-600 text-sm">{csvError}</p>}

            {csvResult?.recommendation && (
              <div className="space-y-3">
                <InvestmentRecommendationCard recommendation={csvResult.recommendation} />
                <div className="flex items-center gap-4 flex-wrap">
                  <button
                    className="px-6 py-2.5 bg-blue-600 text-white rounded-lg font-semibold hover:bg-blue-700 disabled:opacity-50 text-sm transition-colors"
                    onClick={handleConfirmPick}
                    disabled={confirmLoading || !csvResult.recommendation.closePrice}
                  >
                    {confirmLoading ? 'Investing…' : 'Confirm — Invest ₹20,000'}
                  </button>
                  {!csvResult.recommendation.closePrice && (
                    <p className="text-xs text-orange-500">
                      Close price missing in CSV — required to invest.
                    </p>
                  )}
                  {confirmError && <p className="text-red-600 text-sm">{confirmError}</p>}
                </div>
              </div>
            )}
          </div>
        </section>
      )}

      {/* ── Section 3: Position History ──────────────────────────────────── */}
      <section>
        <h2 className="text-lg font-semibold text-slate-700 mb-3">Position History</h2>
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          <PositionHistoryTable positions={history} />
        </div>
      </section>
    </div>
  )
}
