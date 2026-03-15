import { useState, useEffect, useRef } from 'react'
import { listPositions, createMonthlyPick } from '../api/strategyApi'
import { runCsvAnalysis, CsvAnalysisResult } from '../api/csvAnalysisApi'
import { getForecast, getStockPrice, SimulationResult } from '../api/forecastApi'
import InvestmentRecommendationCard from '../components/InvestmentRecommendationCard'
import MonteCarloPanel from '../components/MonteCarloPanel'
import PositionHistoryTable from '../components/PositionHistoryTable'
import SingleStockPicker from '../components/SingleStockPicker'
import { Position } from '../api/strategyApi'

const MODEL_OPTIONS = [
  'MAGIC_FORMULA', 'MULTI_FACTOR', 'WEIGHTED_SCORE', 'PIOTROSKI', 'ALTMAN_Z', 'RELATIVE', 'CUSTOM',
]

// ── Watchlist ─────────────────────────────────────────────────────────────────
interface WatchlistEntry {
  id: string
  symbol: string
  exchange: string
  entryPrice: number
  stopLossPct: number  // e.g. 15 means -15% from entry
  targetPct: number    // e.g. 30 means +30% from entry
}

interface WatchlistItem extends WatchlistEntry {
  forecast: SimulationResult | null
  loading: boolean
  error: string | null
  indicatorCsv: Record<string, string | number>[] | null
  indicatorCsvName: string | null
  indicatorCsvError: string | null
}

const LS_KEY = 'forecast_watchlist_v1'

function loadPersistedWatchlist(): WatchlistEntry[] {
  try {
    const saved: any[] = JSON.parse(localStorage.getItem(LS_KEY) ?? '[]')
    return saved.map(e => ({
      id: e.id,
      symbol: e.symbol,
      exchange: e.exchange,
      entryPrice: e.entryPrice,
      // backward compat: old entries had absolute stopLoss
      stopLossPct: e.stopLossPct ?? (e.stopLoss && e.entryPrice
        ? Math.round((e.entryPrice - e.stopLoss) / e.entryPrice * 100)
        : 15),
      targetPct: e.targetPct ?? 30,
    }))
  } catch {
    return []
  }
}

function persistWatchlist(items: WatchlistEntry[]) {
  localStorage.setItem(LS_KEY, JSON.stringify(items))
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function MonthlyStrategyPage() {
  // ── history / create pick state ─────────────────────────────────────────────
  const [history, setHistory]             = useState<Position[]>([])
  const [loading, setLoading]             = useState(true)
  const [selectedModel, setSelectedModel] = useState('MAGIC_FORMULA')
  const [csvResult, setCsvResult]         = useState<CsvAnalysisResult | null>(null)
  const [csvLoading, setCsvLoading]       = useState(false)
  const [csvError, setCsvError]           = useState<string | null>(null)
  const [confirmLoading, setConfirmLoading] = useState(false)
  const [confirmError, setConfirmError]   = useState<string | null>(null)
  const [forecast, setForecast]           = useState<SimulationResult | null>(null)
  const [forecastLoading, setForecastLoading] = useState(false)
  const [forecastError, setForecastError] = useState<string | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)
  const [indicatorCsv, setIndicatorCsv]         = useState<Record<string, string | number>[]>([])
  const [indicatorCsvName, setIndicatorCsvName]  = useState<string | null>(null)
  const [indicatorCsvError, setIndicatorCsvError] = useState<string | null>(null)

  // ── watchlist state ─────────────────────────────────────────────────────────
  const [watchlist, setWatchlist] = useState<WatchlistItem[]>(() =>
    loadPersistedWatchlist().map(e => ({
      ...e,
      forecast: null, loading: false, error: null,
      indicatorCsv: null, indicatorCsvName: null, indicatorCsvError: null,
    }))
  )
  const [addSymbol,        setAddSymbol]        = useState('')
  const [addExchange,      setAddExchange]      = useState('NSE')
  const [addEntryPrice,    setAddEntryPrice]    = useState('')
  const [addPriceFetching, setAddPriceFetching] = useState(false)
  const [addPriceError,    setAddPriceError]    = useState<string | null>(null)
  const [addStopLossPct,   setAddStopLossPct]   = useState('15')
  const [addTargetPct,     setAddTargetPct]     = useState('30')
  const [addError,         setAddError]         = useState<string | null>(null)

  // Persist watchlist entries (without forecast/csv data) whenever list changes
  useEffect(() => {
    persistWatchlist(watchlist.map(({ id, symbol, exchange, entryPrice, stopLossPct, targetPct }) =>
      ({ id, symbol, exchange, entryPrice, stopLossPct, targetPct })
    ))
  }, [watchlist])

  // ── CSV indicator helpers ───────────────────────────────────────────────────
  const parseIndicatorCsv = (text: string): Record<string, string | number>[] => {
    const lines = text.trim().split('\n').filter(l => l.trim())
    if (lines.length < 2) throw new Error('CSV must have at least a header row and one data row')
    const headers = lines[0].split(',').map(h => h.trim().replace(/^"|"$/g, ''))
    const hasDate   = headers.some(h => h.toLowerCase() === 'date')
    const hasTicker = headers.some(h => h.toLowerCase() === 'ticker')
    if (!hasDate && !hasTicker) {
      throw new Error('CSV must have either a "date" column (time-series) or a "Ticker" column (screener format)')
    }
    return lines.slice(1).map(line => {
      const values = line.match(/(".*?"|[^,]+|(?<=,)(?=,)|(?<=,)$|^(?=,))/g)
        ?.map(v => v.trim().replace(/^"|"$/g, '')) ?? []
      const row: Record<string, string | number> = {}
      headers.forEach((h, i) => {
        const v = values[i] ?? ''
        const isTextCol = h.toLowerCase() === 'date' || h.toLowerCase() === 'name'
          || h.toLowerCase() === 'ticker' || h.toLowerCase().includes('sector')
        row[h] = isTextCol ? v : (v !== '' && !isNaN(Number(v)) ? Number(v) : v)
      })
      return row
    })
  }

  // CSV upload for Create Monthly Pick section
  const handleIndicatorCsvUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setIndicatorCsvError(null)
    try {
      const rows = parseIndicatorCsv(await file.text())
      setIndicatorCsv(rows)
      setIndicatorCsvName(file.name)
    } catch (err: any) {
      setIndicatorCsvError(err.message ?? 'Invalid CSV')
      setIndicatorCsv([])
      setIndicatorCsvName(null)
    }
  }

  // CSV upload for a specific watchlist item — overrides any previous indicators for that item
  const handleWatchlistCsvUpload = async (id: string, file: File) => {
    setWatchlist(prev => prev.map(w =>
      w.id === id ? { ...w, indicatorCsvError: null } : w
    ))
    try {
      const rows = parseIndicatorCsv(await file.text())
      setWatchlist(prev => prev.map(w =>
        w.id === id ? { ...w, indicatorCsv: rows, indicatorCsvName: file.name, indicatorCsvError: null } : w
      ))
    } catch (err: any) {
      setWatchlist(prev => prev.map(w =>
        w.id === id ? { ...w, indicatorCsv: null, indicatorCsvName: null, indicatorCsvError: err.message ?? 'Invalid CSV' } : w
      ))
    }
  }

  // ── data loading ────────────────────────────────────────────────────────────
  const loadData = async () => {
    setLoading(true)
    try {
      const all = await listPositions()
      setHistory(all.filter(p => p.status !== 'ACTIVE'))
    } catch {
      // silently ignore
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
      if (result.recommendation) fetchForecast(result.recommendation)
    } catch (err: any) {
      setCsvError(err.message ?? 'Analysis failed')
    } finally {
      setCsvLoading(false)
    }
  }

  const fetchForecast = async (rec: { symbol: string; closePrice: number | null }) => {
    if (!rec.closePrice) return
    setForecast(null)
    setForecastError(null)
    setForecastLoading(true)
    try {
      const stopLoss = rec.closePrice * 0.85
      const result = await getForecast(rec.symbol, 'NSE', rec.closePrice, stopLoss,
        indicatorCsv.length ? indicatorCsv : undefined)
      setForecast(result)
    } catch (err: any) {
      setForecastError(err.message ?? 'Forecast unavailable')
    } finally {
      setForecastLoading(false)
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
        symbol: rec.symbol, companyName: rec.companyName,
        exchange: 'NSE', entryPrice: rec.closePrice, modelType: selectedModel,
      })
      await loadData()
      setCsvResult(null); setForecast(null); setForecastError(null)
      if (fileRef.current) fileRef.current.value = ''
    } catch (err: any) {
      setConfirmError(err.message ?? 'Failed to create pick')
    } finally {
      setConfirmLoading(false)
    }
  }

  // ── watchlist actions ───────────────────────────────────────────────────────
  const runWatchlistForecast = async (id: string) => {
    const item = watchlist.find(w => w.id === id)
    if (!item) return
    setWatchlist(prev => prev.map(w =>
      w.id === id ? { ...w, loading: true, error: null, forecast: null } : w
    ))
    try {
      const stopLoss = item.entryPrice * (1 - item.stopLossPct / 100)
      const result = await getForecast(
        item.symbol, item.exchange, item.entryPrice, stopLoss,
        item.indicatorCsv ?? undefined,
        item.targetPct,
      )
      setWatchlist(prev => prev.map(w =>
        w.id === id ? { ...w, loading: false, forecast: result } : w
      ))
    } catch (err: any) {
      setWatchlist(prev => prev.map(w =>
        w.id === id ? { ...w, loading: false, error: err.message ?? 'Forecast failed' } : w
      ))
    }
  }

  const handleAddToWatchlist = async () => {
    setAddError(null)
    const sym = addSymbol.trim().toUpperCase()
    if (!sym) { setAddError('Symbol is required'); return }
    const entry  = parseFloat(addEntryPrice)
    const slPct  = parseFloat(addStopLossPct)
    const tgtPct = parseFloat(addTargetPct)
    if (isNaN(entry) || entry <= 0) { setAddError('Valid entry price required'); return }
    if (isNaN(slPct) || slPct <= 0 || slPct >= 100) { setAddError('Stop loss % must be between 1-99'); return }
    if (isNaN(tgtPct) || tgtPct <= 0) { setAddError('Target % must be positive'); return }
    if (watchlist.some(w => w.symbol === sym && w.exchange === addExchange)) {
      setAddError(`${sym} (${addExchange}) is already in the watchlist`); return
    }

    const stopLoss = entry * (1 - slPct / 100)
    const newItem: WatchlistItem = {
      id: `${sym}-${addExchange}-${Date.now()}`,
      symbol: sym, exchange: addExchange,
      entryPrice: entry, stopLossPct: slPct, targetPct: tgtPct,
      forecast: null, loading: true, error: null,
      indicatorCsv: null, indicatorCsvName: null, indicatorCsvError: null,
    }
    setWatchlist(prev => [...prev, newItem])
    setAddSymbol(''); setAddEntryPrice(''); setAddStopLossPct('15'); setAddTargetPct('30')

    try {
      const result = await getForecast(sym, addExchange, entry, stopLoss, undefined, tgtPct)
      setWatchlist(prev => prev.map(w =>
        w.id === newItem.id ? { ...w, loading: false, forecast: result } : w
      ))
    } catch (err: any) {
      setWatchlist(prev => prev.map(w =>
        w.id === newItem.id ? { ...w, loading: false, error: err.message ?? 'Forecast failed' } : w
      ))
    }
  }

  const removeFromWatchlist = (id: string) => {
    setWatchlist(prev => prev.filter(w => w.id !== id))
  }

  // ── render ───────────────────────────────────────────────────────────────────
  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <h1 className="text-2xl font-bold text-slate-800">Monthly Strategy</h1>

      {/* ── Section 1: Forecast Watchlist ─────────────────────────────────── */}
      <section>
        <h2 className="text-lg font-semibold text-slate-700 mb-3">Forecast Watchlist</h2>

        {/* Add form */}
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 mb-4">
          <p className="text-sm text-slate-500 mb-3">
            Add any stock to evaluate its ML forecast — independent of your active investment position.
          </p>
          <div className="flex flex-wrap gap-3 items-start">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-500">Symbol</label>
              <SingleStockPicker
                symbol={addSymbol}
                exchange={addExchange}
                onSelect={async (sym, ex) => {
                  setAddSymbol(sym)
                  setAddExchange(ex)
                  setAddPriceError(null)
                  if (sym) {
                    setAddPriceFetching(true)
                    setAddEntryPrice('')
                    try {
                      const price = await getStockPrice(sym, ex)
                      setAddEntryPrice(String(price))
                    } catch {
                      setAddPriceError('Could not fetch price — enter manually')
                    } finally {
                      setAddPriceFetching(false)
                    }
                  } else {
                    setAddEntryPrice('')
                  }
                }}
              />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-500">
                Entry Price {addPriceFetching && <span className="text-blue-400 font-normal">(fetching…)</span>}
              </label>
              <input
                type="number" min="0" placeholder={addPriceFetching ? 'Fetching…' : 'e.g. 2498.80'}
                value={addEntryPrice}
                onChange={e => { setAddEntryPrice(e.target.value); setAddPriceError(null) }}
                className={`border rounded-lg px-3 py-2 text-sm w-40 focus:outline-none focus:ring-2 focus:ring-blue-400 ${addPriceError ? 'border-orange-300' : 'border-slate-300'}`}
              />
              {addPriceError && <span className="text-xs text-orange-500">{addPriceError}</span>}
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-500">Stop Loss %</label>
              <div className="relative">
                <input
                  type="number" min="1" max="99" placeholder="15"
                  value={addStopLossPct}
                  onChange={e => setAddStopLossPct(e.target.value)}
                  className="border border-slate-300 rounded-lg pl-3 pr-7 py-2 text-sm w-24 focus:outline-none focus:ring-2 focus:ring-blue-400"
                />
                <span className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 text-sm pointer-events-none">%</span>
              </div>
              {addEntryPrice && !isNaN(parseFloat(addEntryPrice)) && !isNaN(parseFloat(addStopLossPct)) && (
                <span className="text-xs text-slate-400">
                  SL ≈ ₹{(parseFloat(addEntryPrice) * (1 - parseFloat(addStopLossPct) / 100)).toFixed(2)}
                </span>
              )}
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-medium text-slate-500">Target %</label>
              <div className="relative">
                <input
                  type="number" min="1" placeholder="30"
                  value={addTargetPct}
                  onChange={e => setAddTargetPct(e.target.value)}
                  className="border border-slate-300 rounded-lg pl-3 pr-7 py-2 text-sm w-24 focus:outline-none focus:ring-2 focus:ring-blue-400"
                />
                <span className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 text-sm pointer-events-none">%</span>
              </div>
              {addEntryPrice && !isNaN(parseFloat(addEntryPrice)) && !isNaN(parseFloat(addTargetPct)) && (
                <span className="text-xs text-slate-400">
                  Target ≈ ₹{(parseFloat(addEntryPrice) * (1 + parseFloat(addTargetPct) / 100)).toFixed(2)}
                </span>
              )}
            </div>
            <button
              onClick={handleAddToWatchlist}
              disabled={addPriceFetching}
              className="px-5 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold hover:bg-blue-700 disabled:opacity-50 transition-colors self-end mb-0.5"
            >
              {addPriceFetching ? 'Fetching price…' : '+ Add & Forecast'}
            </button>
          </div>
          {addError && <p className="text-red-500 text-xs mt-2">{addError}</p>}
        </div>

        {/* Watchlist items */}
        {watchlist.length === 0 ? (
          <div className="bg-slate-50 border border-slate-200 rounded-xl p-6 text-center text-slate-400 text-sm">
            No stocks added yet. Use the form above to add stocks and compare their ML forecasts side-by-side.
          </div>
        ) : (
          <div className="space-y-6">
            {watchlist.map(item => (
              <div key={item.id} className="rounded-xl border border-slate-200 overflow-hidden">
                {/* Item header */}
                <div className="bg-slate-50 border-b border-slate-200 px-5 py-3 flex items-center justify-between gap-3 flex-wrap">
                  <div className="flex items-center gap-3 flex-wrap">
                    <span className="font-bold text-slate-800 text-base">{item.symbol}</span>
                    <span className="text-xs bg-slate-200 text-slate-600 px-2 py-0.5 rounded-full">{item.exchange}</span>
                    <span className="text-xs text-slate-500">
                      Entry ₹{item.entryPrice.toLocaleString()} · SL -{item.stopLossPct}% · Target +{item.targetPct}%
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => runWatchlistForecast(item.id)}
                      disabled={item.loading}
                      className="text-xs px-3 py-1 bg-blue-50 text-blue-600 hover:bg-blue-100 rounded-lg font-medium disabled:opacity-50 transition-colors"
                    >
                      {item.loading ? 'Running…' : '↻ Re-run'}
                    </button>
                    <button
                      onClick={() => removeFromWatchlist(item.id)}
                      className="text-xs px-3 py-1 bg-red-50 text-red-500 hover:bg-red-100 rounded-lg font-medium transition-colors"
                    >
                      Remove
                    </button>
                  </div>
                </div>

                {/* Custom indicators row */}
                <div className="bg-violet-50 border-b border-violet-100 px-5 py-2.5 flex items-center gap-3 flex-wrap">
                  <span className="text-xs font-medium text-violet-700">Custom Indicators</span>
                  <label className="cursor-pointer text-xs px-3 py-1 bg-white border border-violet-200 text-violet-600 hover:bg-violet-50 rounded-lg font-medium transition-colors">
                    {item.indicatorCsvName ? '↺ Replace CSV' : '+ Upload CSV'}
                    <input
                      type="file" accept=".csv" className="hidden"
                      onChange={e => {
                        const file = e.target.files?.[0]
                        if (file) handleWatchlistCsvUpload(item.id, file)
                        e.target.value = ''
                      }}
                    />
                  </label>
                  {item.indicatorCsvName && (
                    <>
                      <span className="text-xs text-violet-600 font-medium">
                        {item.indicatorCsvName} · {item.indicatorCsv?.length} rows
                      </span>
                      <button
                        onClick={() => setWatchlist(prev => prev.map(w =>
                          w.id === item.id ? { ...w, indicatorCsv: null, indicatorCsvName: null, indicatorCsvError: null } : w
                        ))}
                        className="text-xs text-violet-400 hover:text-violet-600 transition-colors"
                      >
                        ✕ Clear
                      </button>
                      <button
                        onClick={() => runWatchlistForecast(item.id)}
                        disabled={item.loading}
                        className="text-xs px-3 py-1 bg-violet-600 text-white hover:bg-violet-700 rounded-lg font-medium disabled:opacity-50 transition-colors"
                      >
                        {item.loading ? 'Running…' : 'Re-run with Indicators'}
                      </button>
                    </>
                  )}
                  {!item.indicatorCsvName && (
                    <span className="text-xs text-slate-400">
                      Screener CSV (with <code className="bg-white px-1 rounded">Ticker</code> column) or time-series CSV (with <code className="bg-white px-1 rounded">date</code> column) — overrides default XGBoost features
                    </span>
                  )}
                  {item.indicatorCsvError && (
                    <span className="text-xs text-red-500">{item.indicatorCsvError}</span>
                  )}
                </div>

                {/* Forecast content */}
                <div className="bg-white">
                  {item.loading && <div className="p-5"><ForecastSkeleton /></div>}
                  {!item.loading && item.error && (
                    <div className="px-5 py-4 text-sm text-orange-500">
                      Forecast unavailable: {item.error}
                      <button
                        onClick={() => runWatchlistForecast(item.id)}
                        className="ml-3 underline text-blue-500"
                      >
                        Retry
                      </button>
                    </div>
                  )}
                  {!item.loading && item.forecast && (
                    <MonteCarloPanel result={item.forecast} />
                  )}
                  {!item.loading && !item.forecast && !item.error && (
                    <div className="px-5 py-4 text-sm text-slate-400">
                      <button
                        onClick={() => runWatchlistForecast(item.id)}
                        className="text-blue-500 underline"
                      >
                        Click to run forecast
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* ── Section 2: Create Monthly Pick ────────────────────────────────── */}
      <section>
        <h2 className="text-lg font-semibold text-slate-700 mb-3">Create Monthly Pick</h2>
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 space-y-4">
          <div className="flex items-center gap-4 flex-wrap">
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium text-slate-600">Model</label>
              <select
                className="border border-slate-300 rounded-lg px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-400"
                value={selectedModel}
                onChange={e => { setSelectedModel(e.target.value); setCsvResult(null); setForecast(null); setForecastError(null) }}
              >
                {MODEL_OPTIONS.map(m => <option key={m} value={m}>{m.replace(/_/g, ' ')}</option>)}
              </select>
            </div>
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium text-slate-600">Upload CSV</label>
              <input
                ref={fileRef} type="file" accept=".csv"
                className="text-sm text-slate-600 file:mr-3 file:py-1.5 file:px-4 file:rounded-lg file:border-0 file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100 cursor-pointer"
                onChange={handleCsvUpload}
              />
            </div>
          </div>

          <div className="flex items-center gap-2">
            <label className="text-sm font-medium text-slate-600">Indicators CSV</label>
            <input
              type="file" accept=".csv"
              className="text-sm text-slate-600 file:mr-3 file:py-1.5 file:px-4 file:rounded-lg file:border-0 file:bg-violet-50 file:text-violet-700 hover:file:bg-violet-100 cursor-pointer"
              onChange={handleIndicatorCsvUpload}
            />
            {indicatorCsvName && (
              <span className="text-xs text-violet-600">{indicatorCsvName} ({indicatorCsv.length} rows)</span>
            )}
          </div>
          {indicatorCsvError && <p className="text-red-500 text-xs">{indicatorCsvError}</p>}

          {csvLoading && <p className="text-slate-400 text-sm">Analysing CSV…</p>}
          {csvError   && <p className="text-red-600 text-sm">{csvError}</p>}

          {csvResult?.recommendation && (
            <div className="space-y-3">
              <InvestmentRecommendationCard recommendation={csvResult.recommendation} />
              {forecastLoading && <ForecastSkeleton />}
              {!forecastLoading && forecastError && (
                <p className="text-xs text-orange-500">Forecast unavailable: {forecastError}</p>
              )}
              {!forecastLoading && forecast && <MonteCarloPanel result={forecast} />}
              <div className="flex items-center gap-4 flex-wrap">
                <button
                  className="px-6 py-2.5 bg-blue-600 text-white rounded-lg font-semibold hover:bg-blue-700 disabled:opacity-50 text-sm transition-colors"
                  onClick={handleConfirmPick}
                  disabled={confirmLoading || !csvResult.recommendation.closePrice}
                >
                  {confirmLoading ? 'Investing…' : 'Confirm — Invest ₹20,000'}
                </button>
                {!csvResult.recommendation.closePrice && (
                  <p className="text-xs text-orange-500">Close price missing in CSV — required to invest.</p>
                )}
                {confirmError && <p className="text-red-600 text-sm">{confirmError}</p>}
              </div>
            </div>
          )}
        </div>
      </section>

      {/* ── Section 3: Position History ───────────────────────────────────── */}
      <section>
        <h2 className="text-lg font-semibold text-slate-700 mb-3">Position History</h2>
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
          {loading ? (
            <p className="text-slate-400 text-sm">Loading…</p>
          ) : (
            <PositionHistoryTable positions={history} />
          )}
        </div>
      </section>
    </div>
  )
}

// ── Shared loading skeleton ───────────────────────────────────────────────────
function ForecastSkeleton() {
  return (
    <div className="bg-slate-50 border border-slate-200 rounded-xl p-5 animate-pulse">
      <div className="h-4 bg-slate-200 rounded w-48 mb-3" />
      <div className="space-y-2">
        <div className="h-3 bg-slate-200 rounded w-full" />
        <div className="h-3 bg-slate-200 rounded w-4/5" />
        <div className="h-3 bg-slate-200 rounded w-3/5" />
      </div>
    </div>
  )
}
