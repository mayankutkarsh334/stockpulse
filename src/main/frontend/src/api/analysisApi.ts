const BASE = '/analysis'

export interface RankedStock {
  symbol: string
  exchange: string
  score: number
  rank: number
  breakdown: Record<string, number>
  rawMetrics: Record<string, number | null>
}

export interface AnalysisResult {
  modelType: string
  rankings: RankedStock[]
  computedAtMs: number
}

export async function runAnalysis(
  symbols: string[],
  exchange: string,
  modelType: string,
  params: Record<string, unknown>
): Promise<AnalysisResult> {
  const res = await fetch(`${BASE}/run`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ symbols, exchange, modelType, params }),
  })
  if (!res.ok) throw new Error(`Analysis failed: ${res.status}`)
  return res.json()
}

export async function getModels(): Promise<string[]> {
  const res = await fetch(`${BASE}/models`)
  if (!res.ok) throw new Error(`Failed to fetch models: ${res.status}`)
  return res.json()
}
