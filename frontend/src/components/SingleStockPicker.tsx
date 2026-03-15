import { useState, useRef, useEffect } from 'react'
import { STOCKS, StockOption } from '../data/stocks'

interface Props {
  symbol: string
  exchange: string
  onSelect: (symbol: string, exchange: string) => void
  placeholder?: string
}

export default function SingleStockPicker({ symbol, exchange, onSelect, placeholder = 'Search symbol or company…' }: Props) {
  const [query, setQuery]   = useState(symbol)
  const [open, setOpen]     = useState(false)
  const containerRef        = useRef<HTMLDivElement>(null)
  const inputRef            = useRef<HTMLInputElement>(null)

  // Keep query in sync when parent resets the symbol
  useEffect(() => { setQuery(symbol) }, [symbol])

  const filtered: StockOption[] = (() => {
    const q = query.toLowerCase()
    if (!q) return STOCKS.slice(0, 50)
    return STOCKS.filter(s =>
      s.symbol.toLowerCase().startsWith(q) ||
      s.name.toLowerCase().includes(q)
    ).slice(0, 50)
  })()

  useEffect(() => {
    function handleMouseDown(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
        // If user typed something that doesn't match a selected stock, reset to last valid
        if (symbol && query !== symbol) setQuery(symbol)
      }
    }
    document.addEventListener('mousedown', handleMouseDown)
    return () => document.removeEventListener('mousedown', handleMouseDown)
  }, [symbol, query])

  function handleSelect(stock: StockOption) {
    onSelect(stock.symbol, stock.exchange)
    setQuery(stock.symbol)
    setOpen(false)
    inputRef.current?.blur()
  }

  function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    setQuery(e.target.value.toUpperCase())
    setOpen(true)
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Escape') { setOpen(false); inputRef.current?.blur() }
    if (e.key === 'Enter' && filtered.length > 0) handleSelect(filtered[0])
  }

  const selectedStock = STOCKS.find(s => s.symbol === symbol && s.exchange === exchange)

  return (
    <div ref={containerRef} className="relative">
      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          className="border border-slate-300 rounded-lg pl-3 pr-8 py-2 text-sm w-52 focus:outline-none focus:ring-2 focus:ring-blue-400 uppercase"
          placeholder={placeholder}
          value={query}
          onChange={handleInputChange}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
          autoComplete="off"
        />
        {/* Clear button */}
        {symbol && (
          <button
            type="button"
            className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 text-lg leading-none"
            onMouseDown={e => { e.preventDefault(); onSelect('', 'NSE'); setQuery(''); setOpen(false) }}
          >
            ×
          </button>
        )}
      </div>

      {/* Selected stock name hint */}
      {selectedStock && !open && (
        <p className="text-xs text-slate-400 mt-0.5 truncate w-52">{selectedStock.name}</p>
      )}

      {/* Dropdown */}
      {open && (
        <ul className="absolute z-50 left-0 mt-1 w-80 bg-white border border-slate-200 rounded-xl shadow-xl max-h-64 overflow-y-auto text-sm">
          {filtered.length > 0 ? filtered.map(stock => (
            <li
              key={stock.symbol + stock.exchange}
              className={`px-3 py-2.5 cursor-pointer flex items-center gap-2 hover:bg-blue-50 ${
                stock.symbol === symbol && stock.exchange === exchange ? 'bg-blue-50 text-blue-700' : ''
              }`}
              onMouseDown={e => { e.preventDefault(); handleSelect(stock) }}
            >
              <span className="font-mono font-semibold w-28 shrink-0 text-slate-800">{stock.symbol}</span>
              <span className="text-slate-500 truncate flex-1">{stock.name}</span>
              <span className="text-xs text-slate-400 shrink-0">{stock.exchange}</span>
            </li>
          )) : (
            <li className="px-3 py-3 text-slate-400 text-center">
              No results for "{query}" — you can still type the symbol manually
            </li>
          )}
        </ul>
      )}
    </div>
  )
}
