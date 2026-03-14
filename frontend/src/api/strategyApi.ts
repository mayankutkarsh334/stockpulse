const BASE = '/strategy/positions'

export interface Position {
  id: string
  symbol: string
  companyName: string
  sector: string | null
  exchange: string
  entryPrice: number
  entryDate: string
  quantity: number
  investedAmount: number
  currentStopLoss: number
  highWaterMarkPrice: number
  pickedMonth: string
  modelType: string
  status: 'ACTIVE' | 'STOPPED_OUT' | 'TARGET_MISSED' | 'MANUALLY_CLOSED'
  closedAt: string | null
  closePrice: number | null
  closePnl: number | null
  closeReason: string | null
  currentGainPct: number
}

export interface CreatePickPayload {
  symbol: string
  companyName: string
  sector?: string
  exchange: string
  entryPrice: number
  modelType: string
}

export interface ManualClosePayload {
  closePrice: number
  notes?: string
}

export async function createMonthlyPick(payload: CreatePickPayload): Promise<Position> {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error ?? `Failed to create pick: ${res.status}`)
  }
  return res.json()
}

export async function listPositions(status?: string): Promise<Position[]> {
  const url = status ? `${BASE}?status=${status}` : BASE
  const res = await fetch(url)
  if (!res.ok) throw new Error(`Failed to list positions: ${res.status}`)
  return res.json()
}

export async function getCurrentPosition(): Promise<Position | null> {
  const res = await fetch(`${BASE}/current`)
  if (res.status === 204) return null
  if (!res.ok) throw new Error(`Failed to get current position: ${res.status}`)
  return res.json()
}

export async function closePosition(id: string, payload: ManualClosePayload): Promise<Position> {
  const res = await fetch(`${BASE}/${id}/close`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!res.ok) throw new Error(`Failed to close position: ${res.status}`)
  return res.json()
}
