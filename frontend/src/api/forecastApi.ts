const BASE = '/forecast'

export interface SimulationResult {
  symbol: string
  ticker: string
  dataPointsUsed: number
  annualizedVolPct: number
  probHitTargetPct: number
  probStopHitPct: number
  probExpiredPct: number
  pct5GainPct: number
  pct50GainPct: number
  pct95GainPct: number
  expectedGainPct: number
  arima30dForecastPct: number | null
  garchVolPct: number | null
  bsImpliedVolPct: number | null
  lstm30dForecastPct: number | null
  xgb30dForecastPct: number | null
  transformer30dForecastPct: number | null
  topFeatures: { name: string; importance_pct: number }[] | null
  numPaths: number
  horizonDays: number
  lstmPricePath: number[] | null
  xgbPricePath: number[] | null
  transformerPricePath: number[] | null
  historicalCloses: number[] | null
  extraFeatureNames: string[] | null
}

export async function getForecast(
  symbol: string,
  exchange: string,
  entryPrice: number,
  stopLoss: number,
  extraIndicators?: Record<string, string | number>[],
  targetPct?: number,
): Promise<SimulationResult> {
  if (extraIndicators && extraIndicators.length > 0) {
    const res = await fetch(`${BASE}/${encodeURIComponent(symbol)}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ exchange, entryPrice, stopLoss, targetPct: targetPct ?? 30, extraIndicators }),
    })
    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      throw new Error(body.error ?? `Forecast failed: ${res.status}`)
    }
    return res.json()
  }

  const params = new URLSearchParams({
    exchange,
    entryPrice: String(entryPrice),
    stopLoss: String(stopLoss),
    targetPct: String(targetPct ?? 30),
  })
  const res = await fetch(`${BASE}/${encodeURIComponent(symbol)}?${params}`)
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error ?? `Forecast failed: ${res.status}`)
  }
  return res.json()
}

export async function getStockPrice(symbol: string, exchange: string): Promise<number> {
  const res = await fetch(`${BASE}/price/${encodeURIComponent(symbol)}?exchange=${exchange}`)
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error ?? `Failed to fetch price: ${res.status}`)
  }
  const data = await res.json()
  return data.price
}
