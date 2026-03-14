export interface AnalysisPreset {
  id: string
  name: string
  description: string
  weights: Record<string, number>
  invertMetrics: string[]
}

export const PRESETS: AnalysisPreset[] = [
  {
    id: 'indian_growth',
    name: 'Indian Growth',
    description: 'Favours high ROCE, ROE, and earnings growth — ideal for quality compounders',
    weights: { ROCE: 0.25, ROE: 0.20, EPS_GROWTH: 0.20, REVENUE_GROWTH: 0.15, DEBT_TO_EQUITY: 0.10, PE: 0.10 },
    invertMetrics: ['PE', 'DEBT_TO_EQUITY'],
  },
  {
    id: 'value',
    name: 'Value Investing',
    description: 'Identifies undervalued stocks via low PE, PB, and strong earnings',
    weights: { PE: 0.30, PB_RATIO: 0.25, EPS_GROWTH: 0.20, REVENUE_GROWTH: 0.15, DEBT_TO_EQUITY: 0.10 },
    invertMetrics: ['PE', 'PB_RATIO', 'DEBT_TO_EQUITY'],
  },
  {
    id: 'quality',
    name: 'Quality & Safety',
    description: 'Prioritises capital efficiency and low leverage',
    weights: { ROCE: 0.30, DEBT_TO_EQUITY: 0.25, ROE: 0.20, PLEDGED_PROMOTER_HOLDINGS: 0.15, EPS_GROWTH: 0.10 },
    invertMetrics: ['DEBT_TO_EQUITY', 'PLEDGED_PROMOTER_HOLDINGS'],
  },
  {
    id: 'momentum',
    name: 'Momentum',
    description: 'Focuses on price momentum and relative strength',
    weights: { RSI_14: 0.25, ONE_YEAR_RETURN: 0.25, SIX_MONTH_RETURN: 0.20, RETURN_VS_NIFTY: 0.20, ONE_MONTH_RETURN: 0.10 },
    invertMetrics: [],
  },
  {
    id: 'custom',
    name: 'Custom',
    description: 'Your own configuration',
    weights: {},
    invertMetrics: [],
  },
]

export const CSV_HEADER_TO_METRIC: Record<string, string> = {
  'pe ratio':                     'PE',
  'return on equity':             'ROE',
  'debt to equity':               'DEBT_TO_EQUITY',
  'roce':                         'ROCE',
  '1y forward eps growth':        'EPS_GROWTH',
  '5y historical revenue growth': 'REVENUE_GROWTH',
  'rsi - 14d':                    'RSI_14',
  'pb ratio':                     'PB_RATIO',
  'pledged promoter holdings':    'PLEDGED_PROMOTER_HOLDINGS',
  'forward pe ratio':             'FORWARD_PE',
  '1m return':                    'ONE_MONTH_RETURN',
  '6m return':                    'SIX_MONTH_RETURN',
  '1y return':                    'ONE_YEAR_RETURN',
  '1y return vs nifty':           'RETURN_VS_NIFTY',
  '% away from 52w high':         'PCT_AWAY_52W_HIGH',
  '50d ema':                      'SMA_50',
  'market cap':                   'MARKET_CAP',
  'close price':                  'CLOSE_PRICE',
}

export interface ModelInfo {
  label: string
  description: string
  requiredMetrics: string[]
  configType: 'weighted' | 'none' | 'relative' | 'custom'
}

export const MODEL_INFO: Record<string, ModelInfo> = {
  WEIGHTED_SCORE: {
    label: 'Weighted Score',
    description: 'Normalised weighted average across chosen metrics.',
    requiredMetrics: [],
    configType: 'weighted',
  },
  PIOTROSKI: {
    label: 'Piotroski F-Score',
    description: 'Evaluates 9 financial health signals (0–9). Higher = stronger.',
    requiredMetrics: ['ROA', 'OPERATING_CASH_FLOW', 'TOTAL_ASSETS', 'DEBT_TO_EQUITY',
                      'CURRENT_RATIO', 'SHARES_OUTSTANDING', 'GROSS_MARGIN',
                      'ASSET_TURNOVER', 'EPS_GROWTH'],
    configType: 'none',
  },
  ALTMAN_Z: {
    label: 'Altman Z-Score',
    description: 'Predicts bankruptcy risk. SAFE > 2.99, GREY 1.81–2.99, DISTRESS < 1.81.',
    requiredMetrics: ['WORKING_CAPITAL', 'TOTAL_ASSETS', 'RETAINED_EARNINGS',
                      'EBIT', 'MARKET_CAP', 'TOTAL_LIABILITIES', 'REVENUE_GROWTH'],
    configType: 'none',
  },
  RELATIVE: {
    label: 'Relative (Sector Z-Score)',
    description: 'Ranks stocks by Z-score vs sector peers across all available CSV metrics.',
    requiredMetrics: [],
    configType: 'relative',
  },
  CUSTOM: {
    label: 'Custom Formula',
    description: 'Write any arithmetic expression using metric names (e.g. ROCE * 0.5 + ROE * 0.5).',
    requiredMetrics: [],
    configType: 'custom',
  },
  MAGIC_FORMULA: {
    label: 'Magic Formula',
    description: "Joel Greenblatt's Magic Formula — ranks by Earnings Yield and Return on Capital. Score normalized 0–1.",
    requiredMetrics: ['EBIT', 'MARKET_CAP', 'TOTAL_LIABILITIES', 'ROCE', 'PE'],
    configType: 'none',
  },
  MULTI_FACTOR: {
    label: 'Multi-Factor Composite',
    description: 'Fama-French inspired model combining Quality, Value, Momentum, and Growth via Z-scores. Score typically −3 to +3.',
    requiredMetrics: ['ROCE', 'ROE', 'PE', 'PB_RATIO', 'ONE_YEAR_RETURN', 'SIX_MONTH_RETURN', 'EPS_GROWTH', 'REVENUE_GROWTH'],
    configType: 'none',
  },
}

export const MODEL_TYPES = ['WEIGHTED_SCORE', 'PIOTROSKI', 'ALTMAN_Z', 'RELATIVE', 'CUSTOM', 'MAGIC_FORMULA', 'MULTI_FACTOR'] as const

export function parseMetricKeysFromCsvHeader(headerLine: string): string[] {
  return headerLine
    .split(',')
    .map(h => h.trim().replace(/[""]/g, '').replace(/\u2013|\u2014/g, '-').toLowerCase())
    .map(h => CSV_HEADER_TO_METRIC[h])
    .filter((k): k is string => !!k)
}
