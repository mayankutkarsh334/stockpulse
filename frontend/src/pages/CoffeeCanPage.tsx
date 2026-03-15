import { useState, useRef } from 'react'
import {
  screenFromCsv,
  getCoffeeCanPortfolios,
  createCoffeeCanPortfolio,
  CoffeeCanScreenResponse,
} from '../api/coffeeCanApi'
import { PortfolioResponse, addTransaction } from '../api/portfolioApi'
import CoffeeCanScreenResults from '../components/CoffeeCanScreenResults'
import CoffeeCanHoldingsTable from '../components/CoffeeCanHoldingsTable'

const DEFAULT_USER = 'user-1'

const MANUAL_CHECKLIST = [
  { label: 'CFO / PAT ≥ 1 over 3 years', note: 'Earnings manipulation red flag' },
  { label: 'Receivables / Revenue stable or declining', note: 'Rising ratio = possible fake revenue' },
  { label: 'Inventory / Revenue stable or declining', note: 'Rising ratio = possible channel stuffing' },
  { label: 'No auditor changes in last 3 years', note: 'Frequent changes signal disputes' },
  { label: 'Related party revenue < 10% of total', note: 'Inflated revenue red flag' },
  { label: 'Contingent liabilities < 30% of net worth', note: 'Hidden balance sheet risk' },
  { label: 'Identifiable competitive moat', note: 'Brand / distribution / switching cost / network / cost advantage' },
  { label: 'Capital allocation track record', note: 'No unnecessary equity dilution, sensible buybacks/dividends' },
  { label: 'Promoter holding ≥ 25%', note: 'Skin in the game' },
  { label: 'Promoter salary reasonable vs profits', note: 'No self-enrichment' },
  { label: 'Oligopolistic / non-commoditised industry', note: 'Pricing power' },
  { label: 'Business easy to understand', note: 'Avoid complexity' },
]

export default function CoffeeCanPage() {
  // Screener state
  const [csvContent, setCsvContent] = useState('')
  const [screening, setScreening] = useState(false)
  const [screenResult, setScreenResult] = useState<CoffeeCanScreenResponse | null>(null)
  const [screenError, setScreenError] = useState<string | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)

  // Manual checklist state
  const [checklistOpen, setChecklistOpen] = useState(false)
  const [checked, setChecked] = useState<boolean[]>(MANUAL_CHECKLIST.map(() => false))

  // Portfolio state
  const [portfolios, setPortfolios] = useState<PortfolioResponse[]>([])
  const [selectedPortfolio, setSelectedPortfolio] = useState<PortfolioResponse | null>(null)
  const [newPortfolioName, setNewPortfolioName] = useState('')
  const [creating, setCreating] = useState(false)
  const [portfolioError, setPortfolioError] = useState<string | null>(null)
  const [portfoliosLoaded, setPortfoliosLoaded] = useState(false)

  // Add holding state
  const [addSymbol, setAddSymbol] = useState('')
  const [addExchange, setAddExchange] = useState('NSE')
  const [addQty, setAddQty] = useState('')
  const [addPrice, setAddPrice] = useState('')
  const [addingHolding, setAddingHolding] = useState(false)

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = ev => setCsvContent(ev.target?.result as string ?? '')
    reader.readAsText(file)
  }

  const runScreen = async () => {
    if (!csvContent.trim()) { setScreenError('Please upload a CSV file first.'); return }
    setScreening(true); setScreenError(null)
    try {
      const result = await screenFromCsv(csvContent)
      setScreenResult(result)
    } catch (e: any) {
      setScreenError(e.message)
    } finally {
      setScreening(false)
    }
  }

  const loadPortfolios = async () => {
    setPortfolioError(null)
    try {
      const list = await getCoffeeCanPortfolios(DEFAULT_USER)
      setPortfolios(list)
      setPortfoliosLoaded(true)
    } catch (e: any) {
      setPortfolioError(e.message)
    }
  }

  const handleCreatePortfolio = async () => {
    if (!newPortfolioName.trim()) return
    setCreating(true); setPortfolioError(null)
    try {
      await createCoffeeCanPortfolio(DEFAULT_USER, newPortfolioName.trim(), 'INR')
      setNewPortfolioName('')
      await loadPortfolios()
    } catch (e: any) {
      setPortfolioError(e.message)
    } finally {
      setCreating(false)
    }
  }

  const handleSelectPortfolio = (p: PortfolioResponse) => {
    setSelectedPortfolio(p)
  }

  const handleAddHolding = async () => {
    if (!selectedPortfolio || !addSymbol || !addQty || !addPrice) return
    setAddingHolding(true)
    try {
      await addTransaction(selectedPortfolio.id, addSymbol, addExchange, 'BUY', parseFloat(addQty), parseFloat(addPrice))
      setAddSymbol(''); setAddQty(''); setAddPrice('')
      // Refresh selected portfolio
      const updated = await getCoffeeCanPortfolios(DEFAULT_USER)
      setPortfolios(updated)
      const refreshed = updated.find(p => p.id === selectedPortfolio.id) ?? null
      setSelectedPortfolio(refreshed)
    } catch (e: any) {
      setPortfolioError(e.message)
    } finally {
      setAddingHolding(false)
    }
  }

  const totalFailed = screenResult
    ? screenResult.totalScanned - screenResult.totalTier1 - screenResult.totalTier2
    : 0

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-slate-800">Coffee Can Investing</h1>
      <p className="text-slate-500 text-sm">Buy quality stocks and hold for 10+ years. ROCE ≥ 15% · Revenue Growth ≥ 10% · Market Cap ≥ ₹500 Cr</p>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* ── Screener ── */}
        <div className="space-y-4">
          <h2 className="text-lg font-semibold text-slate-700">Screener</h2>
          <div className="bg-white rounded-lg shadow p-4 space-y-3">
            <div>
              <label className="block text-sm font-medium text-slate-600 mb-1">Upload CSV</label>
              <input ref={fileRef} type="file" accept=".csv" onChange={handleFileChange}
                className="block w-full text-sm text-slate-500 file:mr-3 file:py-1 file:px-3 file:rounded file:border-0 file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100" />
            </div>
            {csvContent && <p className="text-xs text-green-600">CSV loaded ({csvContent.split('\n').length - 1} rows)</p>}
            <button onClick={runScreen} disabled={screening}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 text-sm font-medium">
              {screening ? 'Screening…' : 'Run Screen'}
            </button>
            {screenError && <p className="text-sm text-red-600">{screenError}</p>}
          </div>

          {screenResult && (
            <div className="space-y-2">
              <p className="text-sm text-slate-600">
                Scanned <strong>{screenResult.totalScanned}</strong> stocks —{' '}
                <strong className="text-green-700">{screenResult.totalTier1} Tier 1</strong>
                {' · '}
                <strong className="text-amber-600">{screenResult.totalTier2} Tier 2</strong>
                {' · '}
                <strong className="text-red-600">{totalFailed} Failed</strong>
              </p>
              <CoffeeCanScreenResults stocks={screenResult.stocks} />
            </div>
          )}

          {/* ── Manual Checklist ── */}
          {screenResult && (
            <div className="bg-white rounded-lg shadow">
              <button
                onClick={() => setChecklistOpen(o => !o)}
                className="w-full flex items-center justify-between px-4 py-3 text-left"
              >
                <span className="text-sm font-semibold text-slate-700">
                  Verify before adding to Coffee Can Portfolio
                </span>
                <span className="text-slate-400 text-xs">{checklistOpen ? '▲ collapse' : '▼ expand'}</span>
              </button>
              {checklistOpen && (
                <div className="px-4 pb-4 space-y-1">
                  <p className="text-xs text-slate-400 mb-3 italic">
                    These qualitative/forensic criteria are from the book but cannot be derived from screener data. Verify manually before buying.
                  </p>
                  {MANUAL_CHECKLIST.map((item, i) => (
                    <label key={i} className="flex items-start gap-2 cursor-pointer group">
                      <input
                        type="checkbox"
                        checked={checked[i]}
                        onChange={() => setChecked(prev => prev.map((v, j) => j === i ? !v : v))}
                        className="mt-0.5 accent-green-600"
                      />
                      <span className={`text-sm ${checked[i] ? 'line-through text-slate-400' : 'text-slate-700'}`}>
                        {item.label}
                        <span className="ml-1 text-xs text-slate-400">— {item.note}</span>
                      </span>
                    </label>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* ── Portfolios ── */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-slate-700">My Coffee Can Portfolios</h2>
            {!portfoliosLoaded && (
              <button onClick={loadPortfolios} className="text-sm text-blue-600 hover:underline">Load</button>
            )}
          </div>

          <div className="bg-white rounded-lg shadow p-4 space-y-3">
            <div className="flex gap-2">
              <input
                value={newPortfolioName}
                onChange={e => setNewPortfolioName(e.target.value)}
                placeholder="New portfolio name"
                className="flex-1 border rounded px-3 py-1.5 text-sm"
              />
              <button onClick={handleCreatePortfolio} disabled={creating || !newPortfolioName.trim()}
                className="px-3 py-1.5 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 text-sm font-medium">
                {creating ? '…' : '+ New'}
              </button>
            </div>
            {portfolioError && <p className="text-sm text-red-600">{portfolioError}</p>}

            {portfolios.length > 0 && (
              <div className="space-y-1">
                {portfolios.map(p => (
                  <button key={p.id} onClick={() => handleSelectPortfolio(p)}
                    className={`w-full text-left px-3 py-2 rounded text-sm ${selectedPortfolio?.id === p.id ? 'bg-blue-50 border border-blue-300' : 'hover:bg-slate-50 border border-transparent'}`}>
                    <span className="font-medium">{p.name}</span>
                    <span className="ml-2 text-slate-400 text-xs">{p.currency} · {p.holdings?.length ?? 0} holdings</span>
                  </button>
                ))}
              </div>
            )}
          </div>

          {selectedPortfolio && (
            <div className="space-y-3">
              <h3 className="font-medium text-slate-700">{selectedPortfolio.name}</h3>

              {/* Add holding form */}
              <div className="bg-white rounded-lg shadow p-4 space-y-2">
                <p className="text-xs font-medium text-slate-500 uppercase tracking-wide">Add Holding</p>
                <div className="flex gap-2 flex-wrap">
                  <input value={addSymbol} onChange={e => setAddSymbol(e.target.value.toUpperCase())}
                    placeholder="Symbol" className="border rounded px-2 py-1 text-sm w-24" />
                  <select value={addExchange} onChange={e => setAddExchange(e.target.value)}
                    className="border rounded px-2 py-1 text-sm">
                    {['NSE', 'BSE', 'NYSE', 'NASDAQ'].map(ex => <option key={ex}>{ex}</option>)}
                  </select>
                  <input value={addQty} onChange={e => setAddQty(e.target.value)}
                    placeholder="Qty" type="number" className="border rounded px-2 py-1 text-sm w-20" />
                  <input value={addPrice} onChange={e => setAddPrice(e.target.value)}
                    placeholder="Price" type="number" className="border rounded px-2 py-1 text-sm w-24" />
                  <button onClick={handleAddHolding} disabled={addingHolding}
                    className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 disabled:opacity-50">
                    {addingHolding ? '…' : 'Add'}
                  </button>
                </div>
              </div>

              {selectedPortfolio.holdings?.length > 0 ? (
                <CoffeeCanHoldingsTable
                  holdings={selectedPortfolio.holdings}
                  currency={selectedPortfolio.currency}
                />
              ) : (
                <p className="text-sm text-slate-400">No holdings yet.</p>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
