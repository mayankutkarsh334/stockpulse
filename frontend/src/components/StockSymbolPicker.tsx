import { useState, useRef, useEffect } from 'react'
import { STOCKS, StockOption } from '../data/stocks'

interface Props {
  exchange: string
  selected: string[]
  onChange: (symbols: string[]) => void
}

export default function StockSymbolPicker({ exchange, selected, onChange }: Props) {
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  const filtered: StockOption[] = (() => {
    const q = query.toLowerCase()
    return STOCKS.filter(s => s.exchange === exchange)
      .filter(s =>
        s.symbol.toLowerCase().startsWith(q) ||
        s.name.toLowerCase().includes(q)
      )
      .slice(0, 50)
  })()

  useEffect(() => {
    function handleMouseDown(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleMouseDown)
    return () => document.removeEventListener('mousedown', handleMouseDown)
  }, [])

  function addSymbol(symbol: string) {
    if (!selected.includes(symbol)) {
      onChange([...selected, symbol])
    }
    setQuery('')
  }

  function removeSymbol(symbol: string) {
    onChange(selected.filter(s => s !== symbol))
  }

  return (
    <div ref={containerRef} className="relative">
      <div
        className="border rounded px-2 py-1.5 flex flex-wrap gap-1 items-center cursor-text bg-white min-h-[42px]"
        onClick={() => setOpen(true)}
      >
        {selected.map(sym => (
          <span key={sym} className="flex items-center gap-1 bg-blue-100 text-blue-800 text-sm px-2 py-0.5 rounded">
            {sym}
            <button
              type="button"
              className="text-blue-500 hover:text-blue-800 leading-none"
              onClick={e => { e.stopPropagation(); removeSymbol(sym) }}
            >
              &times;
            </button>
          </span>
        ))}
        <input
          className="outline-none flex-1 min-w-[80px] text-sm"
          placeholder={selected.length === 0 ? 'Search symbol or company…' : ''}
          value={query}
          onChange={e => { setQuery(e.target.value); setOpen(true) }}
          onFocus={() => setOpen(true)}
          onKeyDown={e => { if (e.key === 'Escape') setOpen(false) }}
        />
        <span className="text-gray-400 text-xs select-none">▾</span>
      </div>

      {open && filtered.length > 0 && (
        <ul className="absolute z-50 left-0 right-0 mt-1 bg-white border rounded shadow-lg max-h-60 overflow-y-auto text-sm">
          {filtered.map(stock => (
            <li
              key={stock.symbol + stock.exchange}
              className={`px-3 py-2 cursor-pointer flex gap-2 hover:bg-blue-50 ${
                selected.includes(stock.symbol) ? 'bg-blue-50 text-blue-700' : ''
              }`}
              onMouseDown={e => { e.preventDefault(); addSymbol(stock.symbol) }}
            >
              <span className="font-mono font-medium w-28 shrink-0">{stock.symbol}</span>
              <span className="text-gray-600">{stock.name}</span>
            </li>
          ))}
        </ul>
      )}

      {open && query.length > 0 && filtered.length === 0 && (
        <div className="absolute z-50 left-0 right-0 mt-1 bg-white border rounded shadow-lg px-3 py-2 text-sm text-gray-500">
          No stocks found for "{query}"
        </div>
      )}
    </div>
  )
}
