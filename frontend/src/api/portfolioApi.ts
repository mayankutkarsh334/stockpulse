const BASE = '/portfolios'

export interface HoldingResponse {
  id: string
  symbol: string
  exchange: string
  quantity: number
  averageBuyPrice: number
  currentPrice: number
  investedValue: number
  currentValue: number
  pnl: number
  pnlPercent: number
  currency: string
  holdDurationDays: number | null
}

export interface PortfolioResponse {
  id: string
  userId: string
  name: string
  currency: string
  totalInvested: number
  totalCurrentValue: number
  totalPnl: number
  totalPnlPercent: number
  holdings: HoldingResponse[]
}

export async function getPortfolio(id: string): Promise<PortfolioResponse> {
  const res = await fetch(`${BASE}/${id}`)
  if (!res.ok) throw new Error(`Failed to fetch portfolio: ${res.status}`)
  return res.json()
}

export async function createPortfolio(userId: string, name: string, currency: string) {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, name, currency }),
  })
  if (!res.ok) throw new Error(`Failed to create portfolio: ${res.status}`)
  return res.json()
}

export async function addTransaction(
  portfolioId: string,
  symbol: string,
  exchange: string,
  type: 'BUY' | 'SELL',
  quantity: number,
  price: number
) {
  const res = await fetch(`${BASE}/${portfolioId}/transactions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ symbol, exchange, type, quantity, price }),
  })
  if (!res.ok) throw new Error(`Failed to add transaction: ${res.status}`)
  return res.json()
}
