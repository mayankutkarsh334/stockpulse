import { useState, useRef } from 'react'
import { runCsvAnalysis, CsvAnalysisResult } from '../api/csvAnalysisApi'
import InvestmentRecommendationCard from '../components/InvestmentRecommendationCard'
import MetricBreakdown from '../components/MetricBreakdown'
import { PRESETS, parseMetricKeysFromCsvHeader, MODEL_INFO, MODEL_TYPES } from '../config/analysisPresets'

const RANK_COLOR = (r: number) =>
  r === 1 ? 'bg-yellow-400' : r === 2 ? 'bg-slate-300' : r === 3 ? 'bg-amber-600 text-white' : 'bg-slate-100'

export default function CsvAnalysisPage() {
  const fileRef = useRef<HTMLInputElement>(null)
  const [csvContent, setCsvContent] = useState('')
  const [fileName, setFileName] = useState('')
  const [availableMetrics, setAvailableMetrics] = useState<string[]>([])
  const [weights, setWeights] = useState<Record<string, number>>({ ...PRESETS[0].weights })
  const [invertMetrics, setInvertMetrics] = useState<string[]>([...PRESETS[0].invertMetrics])
  const [selectedPresetId, setSelectedPresetId] = useState('indian_growth')
  const [addMetricInput, setAddMetricInput] = useState('')
  const [result, setResult] = useState<CsvAnalysisResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [expanded, setExpanded] = useState<string | null>(null)
  const [modelType, setModelType] = useState<string>('WEIGHTED_SCORE')
  const [formula, setFormula] = useState<string>('')

  function handleFile(file: File) {
    setFileName(file.name)
    const reader = new FileReader()
    reader.onload = e => {
      const content = (e.target?.result as string) ?? ''
      setCsvContent(content)
      const firstLine = content.split(/\r?\n/)[0] ?? ''
      setAvailableMetrics(parseMetricKeysFromCsvHeader(firstLine))
    }
    reader.readAsText(file)
  }

  function onFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0]
    if (f) handleFile(f)
  }

  function onDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault()
    const f = e.dataTransfer.files?.[0]
    if (f) handleFile(f)
  }

  function selectPreset(id: string) {
    setSelectedPresetId(id)
    if (id === 'custom') return
    const preset = PRESETS.find(p => p.id === id)
    if (preset) {
      setWeights({ ...preset.weights })
      setInvertMetrics([...preset.invertMetrics])
    }
  }

  function toggleInvert(metric: string) {
    setInvertMetrics(prev =>
      prev.includes(metric) ? prev.filter(m => m !== metric) : [...prev, metric]
    )
    setSelectedPresetId('custom')
  }

  function updateWeight(metric: string, val: string) {
    const num = parseFloat(val)
    if (!isNaN(num)) {
      setWeights(prev => ({ ...prev, [metric]: num }))
      setSelectedPresetId('custom')
    }
  }

  function removeMetric(metric: string) {
    setWeights(prev => {
      const next = { ...prev }
      delete next[metric]
      return next
    })
    setInvertMetrics(prev => prev.filter(m => m !== metric))
    setSelectedPresetId('custom')
  }

  function addMetric(metric: string) {
    if (!metric) return
    setWeights(prev => ({ ...prev, [metric]: 0.05 }))
    setAddMetricInput('')
    setSelectedPresetId('custom')
  }

  const totalWeight = Object.values(weights).reduce((a, b) => a + b, 0)
  const metricsInList = Object.keys(weights)
  const addableMetrics = availableMetrics.filter(m => !metricsInList.includes(m))
  const dataRows = csvContent ? csvContent.split(/\r?\n/).filter(l => l.trim()).length - 1 : 0

  async function handleAnalyze() {
    if (!csvContent.trim()) { setError('Please upload a CSV file first.'); return }
    if (modelType === 'WEIGHTED_SCORE') {
      const missing = availableMetrics.length > 0
        ? Object.keys(weights).filter(m => !availableMetrics.includes(m))
        : []
      if (missing.length > 0) {
        setError(`Metrics not found in CSV: ${missing.join(', ')}`)
        return
      }
    }
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const data = await runCsvAnalysis({
        csvContent,
        modelType,
        weights: modelType === 'WEIGHTED_SCORE' ? weights : undefined,
        invertMetrics: modelType === 'WEIGHTED_SCORE' ? invertMetrics : undefined,
        formula: modelType === 'CUSTOM' ? formula : undefined,
      })
      setResult(data)
    } catch (e) {
      setError(String(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-5xl mx-auto">
      <h1 className="text-2xl font-bold mb-2">CSV Stock Analysis</h1>
      <p className="text-slate-500 text-sm mb-6">
        Upload a Gemini / broker screener CSV export. All metrics are read directly from the
        file — no API calls needed.
      </p>

      {/* 1. Upload CSV */}
      <div className="bg-white rounded-lg shadow p-6 mb-4">
        <h2 className="font-semibold mb-3">1. Upload CSV</h2>
        <div
          className="border-2 border-dashed border-slate-300 rounded-lg p-8 text-center cursor-pointer hover:border-blue-400 transition-colors"
          onDrop={onDrop}
          onDragOver={e => e.preventDefault()}
          onClick={() => fileRef.current?.click()}
        >
          <input ref={fileRef} type="file" accept=".csv" className="hidden" onChange={onFileChange} />
          {fileName ? (
            <p className="text-green-700 font-medium">{fileName}</p>
          ) : (
            <>
              <p className="text-slate-500">Drag & drop a .csv file here, or click to browse</p>
              <p className="text-xs text-slate-400 mt-1">Supports Gemini Screener export format</p>
            </>
          )}
        </div>
        {csvContent && (
          <p className="text-xs text-slate-400 mt-2">
            {dataRows} data rows loaded · {availableMetrics.length} metrics detected
          </p>
        )}
      </div>

      {/* 2. Configure Analysis */}
      <div className="bg-white rounded-lg shadow p-6 mb-4">
        <h2 className="font-semibold mb-4">2. Configure Analysis</h2>

        {/* Model selector */}
        <div className="flex items-start gap-4 mb-5">
          <div>
            <label className="text-sm font-medium block mb-1">Analysis Model</label>
            <select
              value={modelType}
              onChange={e => setModelType(e.target.value)}
              className="border rounded px-3 py-2"
            >
              {MODEL_TYPES.map(m => (
                <option key={m} value={m}>{MODEL_INFO[m].label}</option>
              ))}
            </select>
          </div>
          <div className="flex-1 pt-7">
            <p className="text-sm text-slate-500">{MODEL_INFO[modelType]?.description ?? ''}</p>
          </div>
        </div>

        <hr className="mb-4" />

        {/* Conditional config panels */}
        {MODEL_INFO[modelType]?.configType === 'weighted' && (
          <>
            {/* Preset selector */}
            <div className="flex items-start gap-4 mb-5">
              <div>
                <label className="text-sm font-medium block mb-1">Preset</label>
                <select
                  value={selectedPresetId}
                  onChange={e => selectPreset(e.target.value)}
                  className="border rounded px-3 py-2"
                >
                  {PRESETS.map(p => (
                    <option key={p.id} value={p.id}>{p.name}</option>
                  ))}
                </select>
              </div>
              <div className="flex-1 pt-7">
                <p className="text-sm text-slate-500">
                  {PRESETS.find(p => p.id === selectedPresetId)?.description ?? ''}
                </p>
              </div>
            </div>

            <hr className="mb-4" />

            {/* Metric weights */}
            <div className="flex justify-between mb-2">
              <span className="text-sm font-medium">Metric Weights</span>
              <span className={`text-xs font-mono ${Math.abs(totalWeight - 1) > 0.01 ? 'text-red-500' : 'text-green-600'}`}>
                Sum: {totalWeight.toFixed(2)}
              </span>
            </div>
            <div className="space-y-2 mb-4">
              {Object.entries(weights).map(([metric, w]) => (
                <div key={metric} className="flex items-center gap-2">
                  <span className="text-xs font-mono w-44 text-slate-700">{metric}</span>
                  <input
                    type="range" min="0" max="0.5" step="0.01"
                    value={w}
                    onChange={e => updateWeight(metric, e.target.value)}
                    className="flex-1 h-1.5 accent-blue-600"
                  />
                  <span className="text-xs font-mono w-10 text-right">{(w * 100).toFixed(0)}%</span>
                  <button
                    onClick={() => toggleInvert(metric)}
                    title={invertMetrics.includes(metric) ? 'Lower is better (click to toggle)' : 'Higher is better (click to toggle)'}
                    className={`text-xs px-1.5 py-0.5 rounded border transition-colors ${
                      invertMetrics.includes(metric)
                        ? 'bg-orange-100 border-orange-400 text-orange-600'
                        : 'bg-slate-50 border-slate-200 text-slate-400 hover:border-slate-400'
                    }`}
                  >
                    ↓
                  </button>
                  <button
                    onClick={() => removeMetric(metric)}
                    className="text-xs px-1.5 py-0.5 rounded border border-slate-200 text-slate-400 hover:border-red-400 hover:text-red-500 transition-colors"
                    title="Remove metric"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>

            {/* Add Metric */}
            <div className="flex items-center gap-2 mb-6">
              <select
                value={addMetricInput}
                onChange={e => setAddMetricInput(e.target.value)}
                disabled={!csvContent}
                title={!csvContent ? 'Upload a CSV first' : undefined}
                className="border rounded px-3 py-1.5 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <option value="">+ Add Metric</option>
                {addableMetrics.map(m => (
                  <option key={m} value={m}>{m}</option>
                ))}
              </select>
              {addMetricInput && (
                <button
                  onClick={() => addMetric(addMetricInput)}
                  className="text-sm bg-blue-50 border border-blue-300 text-blue-700 px-3 py-1.5 rounded hover:bg-blue-100"
                >
                  Add
                </button>
              )}
            </div>
          </>
        )}

        {MODEL_INFO[modelType]?.configType === 'none' && (
          <div className="bg-slate-50 border border-slate-200 rounded-lg p-4 mb-6">
            <p className="text-sm text-slate-600 mb-3">No configuration needed.</p>
            {MODEL_INFO[modelType].requiredMetrics.length > 0 && (
              <>
                <p className="text-xs font-medium text-slate-500 mb-2">Required metrics:</p>
                <div className="flex flex-wrap gap-2">
                  {MODEL_INFO[modelType].requiredMetrics.map(m => {
                    const present = availableMetrics.includes(m)
                    return (
                      <span
                        key={m}
                        className={`text-xs px-2 py-1 rounded font-mono ${
                          present
                            ? 'bg-green-100 text-green-700 border border-green-200'
                            : 'bg-red-50 text-red-500 border border-red-200'
                        }`}
                      >
                        {present ? '✓' : '✗'} {m}
                      </span>
                    )
                  })}
                </div>
                {!csvContent && (
                  <p className="text-xs text-slate-400 mt-2">Upload a CSV to see which metrics are available.</p>
                )}
              </>
            )}
          </div>
        )}

        {MODEL_INFO[modelType]?.configType === 'relative' && (
          <div className="bg-slate-50 border border-slate-200 rounded-lg p-4 mb-6">
            <p className="text-sm text-slate-600 mb-3">
              Automatically uses all metrics detected in your CSV as comparison dimensions.
            </p>
            {availableMetrics.length > 0 ? (
              <div className="flex flex-wrap gap-2">
                {availableMetrics.map(m => (
                  <span key={m} className="text-xs px-2 py-1 rounded font-mono bg-blue-50 text-blue-700 border border-blue-200">
                    {m}
                  </span>
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-400">Upload a CSV to see detected metrics.</p>
            )}
          </div>
        )}

        {MODEL_INFO[modelType]?.configType === 'custom' && (
          <div className="mb-6">
            <label className="text-sm font-medium block mb-1">Formula</label>
            <input
              type="text"
              value={formula}
              onChange={e => setFormula(e.target.value)}
              placeholder="e.g. ROCE * 0.5 + ROE * 0.3 - PE * 0.2"
              className="w-full border rounded px-3 py-2 text-sm font-mono"
            />
            {availableMetrics.length > 0 && (
              <p className="text-xs text-slate-500 mt-2">
                Available metrics: {availableMetrics.join(' · ')}
              </p>
            )}
          </div>
        )}

        <hr className="mb-4" />

        <button
          onClick={handleAnalyze}
          disabled={loading || !csvContent}
          className="bg-blue-600 text-white px-8 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? 'Analyzing...' : 'Analyze & Recommend'}
        </button>
      </div>

      {error && <div className="text-red-600 mb-4">{error}</div>}

      {/* Results */}
      {result && (
        <div>
          <p className="text-sm text-slate-500 mb-4">
            {result.totalStocksAnalyzed} stocks analyzed
          </p>

          {result.recommendation && (
            <InvestmentRecommendationCard recommendation={result.recommendation} />
          )}

          <h2 className="text-lg font-semibold mb-3">Full Rankings</h2>
          <div className="space-y-3">
            {result.rankings.map(stock => (
              <div key={stock.symbol} className="bg-white rounded-lg shadow p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${RANK_COLOR(stock.rank)}`}>
                      #{stock.rank}
                    </span>
                    <div>
                      <div className="font-semibold">{stock.companyName}</div>
                      <div className="text-xs text-slate-500">
                        {stock.symbol}
                        {stock.sector && <span className="ml-2 text-slate-400">· {stock.sector}</span>}
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-xl font-bold text-blue-600">{stock.score.toFixed(3)}</div>
                    {stock.closePrice != null && (
                      <div className="text-xs text-slate-500">
                        ₹{stock.closePrice.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                      </div>
                    )}
                    <button
                      className="text-xs text-slate-500 hover:text-blue-600 mt-0.5"
                      onClick={() => setExpanded(expanded === stock.symbol ? null : stock.symbol)}
                    >
                      {expanded === stock.symbol ? 'Hide' : 'Show'} breakdown
                    </button>
                  </div>
                </div>
                {expanded === stock.symbol && (
                  <MetricBreakdown breakdown={stock.breakdown} rawMetrics={stock.rawMetrics} />
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
