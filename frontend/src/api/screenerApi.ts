const BASE = '/screener'

export interface FilterCriteria {
  indicator: string
  operator: string
  value: number
}

export interface ScreenerMatch {
  symbol: string
  exchange: string
  metricValues: Record<string, number>
  passedFilters: string[]
}

export interface ScreenerResult {
  matches: ScreenerMatch[]
  totalScanned: number
  totalMatched: number
}

export async function runScan(
  symbols: string[],
  exchange: string,
  filters: FilterCriteria[]
): Promise<ScreenerResult> {
  const res = await fetch(`${BASE}/scan`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ symbols, exchange, filters }),
  })
  if (!res.ok) throw new Error(`Screener scan failed: ${res.status}`)
  return res.json()
}

export async function getPresets(): Promise<string[]> {
  const res = await fetch(`${BASE}/presets`)
  if (!res.ok) throw new Error(`Failed to fetch presets: ${res.status}`)
  return res.json()
}
