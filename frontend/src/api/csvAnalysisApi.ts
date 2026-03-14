const BASE = '/analysis/csv'

export interface CsvAnalysisRequest {
  csvContent: string
  modelType?: string
  weights?: Record<string, number>
  invertMetrics?: string[]
  formula?: string
}

export interface CsvRankedStock {
  symbol: string
  companyName: string
  sector: string | null
  score: number
  rank: number
  breakdown: Record<string, number>
  rawMetrics: Record<string, number | null>
  closePrice: number | null
}

export interface InvestmentRecommendation {
  symbol: string
  companyName: string
  rank: number
  score: number
  confidenceTier: 'HIGH' | 'MEDIUM' | 'LOW'
  closePrice: number | null
  rationale: string
}

export interface CsvAnalysisResult {
  rankings: CsvRankedStock[]
  recommendation: InvestmentRecommendation | null
  availableMetrics: string[]
  computedAtMs: number
  totalStocksAnalyzed: number
}

export async function runCsvAnalysis(req: CsvAnalysisRequest): Promise<CsvAnalysisResult> {
  const res = await fetch(`${BASE}/run`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error ?? `CSV analysis failed: ${res.status}`)
  }
  return res.json()
}
