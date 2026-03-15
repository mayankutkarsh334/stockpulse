import { PortfolioResponse } from './portfolioApi'

const BASE = 'http://localhost:8080'

export interface CoffeeCanStock {
  symbol: string
  companyName: string
  roce: number | null
  debtToEquity: number | null
  revenueGrowth: number | null
  pledgedPromoterHoldings: number | null
  pbRatio: number | null
  marketCap: number | null
  tier: 'TIER_1' | 'TIER_2' | 'FAIL'
  failReasons: string[]
}

export interface CoffeeCanScreenResponse {
  stocks: CoffeeCanStock[]
  totalScanned: number
  totalTier1: number
  totalTier2: number
}

export async function screenFromCsv(csvContent: string): Promise<CoffeeCanScreenResponse> {
  const res = await fetch(`${BASE}/coffee-can/screen`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ csvContent }),
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.message ?? `Screener failed: ${res.status}`)
  }
  return res.json()
}

export async function getCoffeeCanPortfolios(userId: string): Promise<PortfolioResponse[]> {
  const res = await fetch(`${BASE}/portfolios?userId=${encodeURIComponent(userId)}&type=COFFEE_CAN`)
  if (!res.ok) throw new Error(`Failed to fetch coffee can portfolios: ${res.status}`)
  return res.json()
}

export async function createCoffeeCanPortfolio(userId: string, name: string, currency: string) {
  const res = await fetch(`${BASE}/portfolios`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, name, currency, type: 'COFFEE_CAN' }),
  })
  if (!res.ok) throw new Error(`Failed to create coffee can portfolio: ${res.status}`)
  return res.json()
}
