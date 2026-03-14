import { InvestmentRecommendation } from '../api/csvAnalysisApi'

interface Props {
  recommendation: InvestmentRecommendation
}

const CONFIDENCE_STYLES: Record<string, string> = {
  HIGH:   'bg-green-100 text-green-800 border-green-300',
  MEDIUM: 'bg-yellow-100 text-yellow-800 border-yellow-300',
  LOW:    'bg-red-100 text-red-800 border-red-300',
}

export default function InvestmentRecommendationCard({ recommendation: r }: Props) {
  const confStyle = CONFIDENCE_STYLES[r.confidenceTier] ?? CONFIDENCE_STYLES.MEDIUM

  return (
    <div className="bg-white rounded-xl shadow-md border-l-4 border-blue-500 p-6 mb-6">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <p className="text-xs font-semibold text-slate-400 uppercase tracking-wide mb-1">
            Recommended Investment
          </p>
          <h2 className="text-2xl font-bold text-slate-800">{r.companyName}</h2>
          <p className="text-slate-500 text-sm font-mono mt-0.5">{r.symbol}</p>
        </div>
        <div className="flex flex-col items-end gap-2">
          <span className={`px-3 py-1 rounded-full text-xs font-bold border ${confStyle}`}>
            {r.confidenceTier}
          </span>
          <span className="text-2xl font-bold text-blue-600">{r.score.toFixed(3)}</span>
          <span className="text-xs text-slate-400">Composite score</span>
        </div>
      </div>

      <p className="mt-4 text-sm text-slate-600 italic">{r.rationale}</p>
    </div>
  )
}
