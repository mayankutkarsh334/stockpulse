import React from 'react'
import { SimulationResult } from '../api/forecastApi'

interface Props {
  result: SimulationResult
}

// ── Tooltip ───────────────────────────────────────────────────────────────────
function InfoTip({ text }: { text: string }) {
  return (
    <span className="relative group inline-flex items-center ml-1 cursor-help align-middle">
      <svg
        className="w-3.5 h-3.5 text-slate-300 group-hover:text-slate-500 transition-colors"
        fill="currentColor" viewBox="0 0 20 20"
      >
        <path fillRule="evenodd" d="M18 10A8 8 0 11 2 10a8 8 0 0116 0zm-8-3a1 1 0 00-.867.5 1 1 0 11-1.731-1A3 3 0 0113 10a3 3 0 01-2 2.83V13a1 1 0 11-2 0v-1.5a1 1 0 011-1 1 1 0 100-2zm0 8a1 1 0 100-2 1 1 0 000 2z" clipRule="evenodd" />
      </svg>
      <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-60 bg-slate-800 text-white text-xs rounded-lg px-3 py-2 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none z-50 leading-relaxed shadow-xl whitespace-normal text-left">
        {text}
        <span className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-slate-800" />
      </span>
    </span>
  )
}

// ── Feature name descriptions ─────────────────────────────────────────────────
const FEATURE_TIPS: Record<string, string> = {
  log_ret_1d:        'Yesterday\'s log return — the single-day price change on a log scale.',
  log_ret_5d:        'Cumulative log return over the past 5 trading days (1 week).',
  log_ret_21d:       'Cumulative log return over the past 21 trading days (1 month).',
  volatility_21d:    'Standard deviation of daily log returns over 21 days. High = more uncertain short-term price swings.',
  volatility_63d:    'Standard deviation of daily log returns over 63 days (1 quarter). Key indicator of medium-term risk.',
  momentum_5d:       '5-day price momentum: how much the stock moved in the past week relative to where it started.',
  momentum_21d:      '21-day price momentum: 1-month directional trend strength.',
  momentum_63d:      '63-day price momentum: 3-month directional trend — a strong predictor of near-term continuation.',
  rsi_14d:           'Relative Strength Index over 14 days. Above 70 = overbought, below 30 = oversold.',
  ema_ratio_20:      'Price divided by the 20-day Exponential Moving Average. >1 means price is above its short-term trend.',
  ema_ratio_ema2050: 'EMA20 divided by EMA50. >1 = short-term trend is above long-term trend (bullish crossover signal).',
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function ProbBar({ label, tip, pct, color }: { label: string; tip: string; pct: number; color: string }) {
  return (
    <div className="flex items-center gap-3">
      <span className="w-44 text-sm text-slate-600 shrink-0 flex items-center">
        {label}<InfoTip text={tip} />
      </span>
      <div className="flex-1 bg-slate-100 rounded-full h-3 overflow-hidden">
        <div
          className={`h-3 rounded-full ${color}`}
          style={{ width: `${Math.min(pct, 100)}%` }}
        />
      </div>
      <span className="w-14 text-right text-sm font-semibold text-slate-700">
        {pct.toFixed(1)}%
      </span>
    </div>
  )
}

function fmt(n: number | null | undefined, suffix = '%'): string {
  if (n == null) return 'n/a'
  const sign = n > 0 ? '+' : ''
  const decimals = Math.abs(n) < 0.1 ? 2 : 1
  return `${sign}${n.toFixed(decimals)}${suffix}`
}

function fmtForecast(n: number | null | undefined): React.ReactNode {
  if (n == null) return <span className="text-slate-400 text-xs">training…</span>
  const sign = n > 0 ? '+' : ''
  const color = n > 0 ? 'text-emerald-600' : n < 0 ? 'text-red-500' : 'text-slate-700'
  return <span className={`font-semibold ${color}`}>{sign}{n.toFixed(1)}%</span>
}

function ImportanceBar({ name, pct, maxPct }: { name: string; pct: number; maxPct: number }) {
  const barWidth = maxPct > 0 ? (pct / maxPct) * 100 : 0
  const tip = FEATURE_TIPS[name]
  return (
    <div className="flex items-center gap-2 text-xs">
      <span className="w-40 text-slate-600 shrink-0 truncate flex items-center">
        {name}{tip && <InfoTip text={tip} />}
      </span>
      <div className="flex-1 bg-slate-100 rounded h-2 overflow-hidden">
        <div
          className="h-2 rounded bg-violet-400"
          style={{ width: `${barWidth}%` }}
        />
      </div>
      <span className="w-10 text-right text-slate-500">{pct.toFixed(1)}%</span>
    </div>
  )
}

// ── Main component ────────────────────────────────────────────────────────────
export default function MonteCarloPanel({ result }: Props) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
      {/* Header */}
      <div className="bg-slate-50 border-b border-slate-200 px-5 py-3 flex items-center justify-between">
        <span className="font-semibold text-slate-700 flex items-center">
          Monte Carlo Simulation
          <InfoTip text="Runs 10,000 simulated price paths using Geometric Brownian Motion (GBM). Each path randomly evolves the stock price based on its historical drift and volatility. The statistics below summarise where those paths ended up." />
        </span>
        <span className="text-xs text-slate-400">
          {result.numPaths.toLocaleString()} paths · 2yr horizon · {result.dataPointsUsed} data pts (5yr history)
        </span>
      </div>

      <div className="px-5 py-4 space-y-4">
        {/* Probability bars */}
        <div className="space-y-2.5">
          <ProbBar
            label="Hit +30% target 🟢"
            tip="Percentage of simulated paths that reached a +30% gain before hitting the stop loss or the 2-year time limit. Higher is better — it means more simulations resulted in a profitable exit."
            pct={result.probHitTargetPct}
            color="bg-emerald-500"
          />
          <ProbBar
            label="Stop loss first 🔴"
            tip="Percentage of paths that hit your stop-loss price before reaching the +30% target. This is your simulated downside risk — the chance the trade closes at a loss."
            pct={result.probStopHitPct}
            color="bg-red-400"
          />
          <ProbBar
            label="Expired neutral ⬜"
            tip="Percentage of paths that neither hit the target nor the stop loss within 2 years — the trade just drifted. The final gain/loss in these cases is captured in the percentile stats below."
            pct={result.probExpiredPct}
            color="bg-slate-400"
          />
        </div>

        <hr className="border-slate-100" />

        {/* Stats grid */}
        <div className="grid grid-cols-2 gap-x-6 gap-y-1.5 text-sm">
          <div className="flex justify-between items-center">
            <span className="text-slate-500 flex items-center">
              Volatility (ann.)
              <InfoTip text="Historical annualised volatility — how much the stock's daily returns vary, scaled to a full year. Calculated as the standard deviation of daily log returns × √252. Higher volatility means wider price swings and more uncertain outcomes." />
            </span>
            <span className="font-medium text-slate-700">{fmt(result.annualizedVolPct)}</span>
          </div>

          <div className="flex justify-between items-center">
            <span className="text-slate-500 flex items-center">
              GARCH est.
              <InfoTip text="GARCH(1,1) conditional volatility — a more sophisticated estimate that weights recent returns more heavily. Unlike historical vol, GARCH adapts quickly when the market becomes turbulent. Useful for gauging current risk levels." />
            </span>
            <span className="font-medium text-slate-700">{fmt(result.garchVolPct)}</span>
          </div>

          <div className="flex justify-between items-center">
            <span className="text-slate-500 flex items-center">
              BS implied vol
              <InfoTip text="Black-Scholes Implied Volatility — the market's forward-looking volatility expectation extracted from live options prices. If options traders expect a big move, IV is high. Unavailable for NSE/BSE stocks since Yahoo Finance doesn't carry Indian options data." />
            </span>
            <span className="font-medium text-slate-700">
              {result.bsImpliedVolPct != null
                ? fmt(result.bsImpliedVolPct)
                : <span className="text-slate-400 text-xs">n/a (NSE options not on Yahoo)</span>}
            </span>
          </div>

          <div className="flex justify-between items-center">
            <span className="text-slate-500 flex items-center">
              ARIMA 30d forecast
              <InfoTip text="ARIMA(1,1,0) time-series model trained on log prices. It extrapolates the recent trend and mean-reversion pattern 30 days forward and expresses the result as % gain/loss from your entry price. Best for capturing short-term momentum." />
            </span>
            <span className="font-medium text-slate-700">{fmt(result.arima30dForecastPct)}</span>
          </div>

          <div className="flex justify-between items-center">
            <span className="text-slate-500 flex items-center">
              Expected gain
              <InfoTip text="The probability-weighted average gain across all 10,000 simulated paths at the 2-year mark. This is the mean outcome — not a guarantee — and accounts for all scenarios including stop-loss hits." />
            </span>
            <span className="font-medium text-slate-700">{fmt(result.expectedGainPct)}</span>
          </div>

          <div className="flex justify-between items-center col-span-2">
            <span className="text-slate-500 flex items-center">
              Percentiles at yr2
              <InfoTip text="P5 / P50 / P95 final gains across all simulated paths at the 2-year horizon. P5 = worst 5% of outcomes (downside risk). P50 = median outcome (most likely). P95 = best 5% of outcomes (upside potential)." />
            </span>
            <span className="font-medium text-slate-700">
              P5: {fmt(result.pct5GainPct)}&nbsp;&nbsp;P50: {fmt(result.pct50GainPct)}&nbsp;&nbsp;P95: {fmt(result.pct95GainPct)}
            </span>
          </div>
        </div>

        <hr className="border-slate-100" />

        {/* ML Model Forecasts */}
        <div>
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2 flex items-center">
            ML Model Forecasts (30d)
            <InfoTip text="Three independent machine-learning models — each trained on 5 years of price history — predict the stock's % gain 30 days from today. They use different techniques, so divergence between them signals uncertainty; agreement signals conviction." />
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div className="bg-slate-50 rounded-lg px-3 py-2 text-center">
              <div className="text-xs text-slate-400 mb-1 flex items-center justify-center">
                LSTM
                <InfoTip text="Long Short-Term Memory — a recurrent neural network that reads price return sequences in order, day by day. It 'remembers' patterns over many months (e.g. slow trends, seasonality) and uses them to predict the next 30 returns autoregressively." />
              </div>
              <div className="text-sm">{fmtForecast(result.lstm30dForecastPct)}</div>
            </div>
            <div className="bg-slate-50 rounded-lg px-3 py-2 text-center">
              <div className="text-xs text-slate-400 mb-1 flex items-center justify-center">
                XGBoost
                <InfoTip text="Gradient-boosted decision trees trained on 11 technical indicators (momentum, volatility, RSI, EMA ratios). Unlike neural models, XGBoost looks at the current state of those indicators — not the sequence — and finds non-linear relationships between them and future returns." />
              </div>
              <div className="text-sm">{fmtForecast(result.xgb30dForecastPct)}</div>
            </div>
            <div className="bg-slate-50 rounded-lg px-3 py-2 text-center">
              <div className="text-xs text-slate-400 mb-1 flex items-center justify-center">
                Transformer
                <InfoTip text="PatchTST-style attention model — splits the return series into 16-day patches and uses self-attention to compare all time windows simultaneously. Unlike LSTM, it can instantly relate patterns from 3 months ago to patterns from last week without reading sequentially." />
              </div>
              <div className="text-sm">{fmtForecast(result.transformer30dForecastPct)}</div>
            </div>
          </div>
        </div>

        {/* XGBoost Feature Importance */}
        {result.topFeatures && result.topFeatures.length > 0 && (
          <div>
            <div className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2 flex items-center">
              XGBoost Feature Importance
              <InfoTip text="Shows which technical indicators contributed most to XGBoost's prediction, measured by 'gain' — the average improvement in accuracy each feature provides across all decision tree splits. A high bar means the model relied heavily on that feature to make its call." />
            </div>
            <div className="space-y-1.5">
              {(() => {
                const maxPct = Math.max(...result.topFeatures!.map(f => f.importance_pct))
                return result.topFeatures!.map(f => (
                  <ImportanceBar key={f.name} name={f.name} pct={f.importance_pct} maxPct={maxPct} />
                ))
              })()}
            </div>
          </div>
        )}

        {/* Extra indicators used in training */}
        {result.extraFeatureNames && result.extraFeatureNames.length > 0 && (
          <div className="text-xs text-slate-400 bg-violet-50 rounded-lg px-3 py-2">
            Extra indicators used in training: {result.extraFeatureNames.join(', ')}
          </div>
        )}

      </div>
    </div>
  )
}
