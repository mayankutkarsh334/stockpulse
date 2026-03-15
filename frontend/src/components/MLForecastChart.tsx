import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js'
import { Line } from 'react-chartjs-2'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend)

interface Props {
  historicalCloses: number[]   // ~252 actual prices
  lstmPath: number[] | null    // 504 forecast prices
  xgbPath: number[] | null
  transformerPath: number[] | null
  entryPrice: number
}

const todayLinePlugin = {
  id: 'todayLine',
  afterDraw(chart: any) {
    const ctx = chart.ctx
    const xAxis = chart.scales.x
    const yAxis = chart.scales.y
    const todayIndex = (chart as any)._todayIndex as number
    if (todayIndex >= xAxis.min && todayIndex <= xAxis.max) {
      const x = xAxis.getPixelForValue(todayIndex)
      ctx.save()
      ctx.beginPath()
      ctx.moveTo(x, yAxis.top)
      ctx.lineTo(x, yAxis.bottom)
      ctx.lineWidth = 1.5
      ctx.strokeStyle = 'rgba(100,116,139,0.5)'
      ctx.setLineDash([4, 4])
      ctx.stroke()
      ctx.restore()
    }
  },
}

export default function MLForecastChart({
  historicalCloses,
  lstmPath,
  xgbPath,
  transformerPath,
  entryPrice,
}: Props) {
  const histLen = historicalCloses.length
  const forecastLen = 504

  // Convert an absolute price to % gain from entry price
  const toGainPct = (price: number) => (price - entryPrice) / entryPrice * 100

  // x-axis labels: Day -histLen+1 ... Day 0 (history), Day 1 ... Day 504 (forecast)
  const labels: string[] = []
  for (let i = -(histLen - 1); i <= forecastLen; i++) {
    labels.push(String(i))
  }

  // Historical data as % gain from entry price
  const histData: (number | null)[] = [
    ...historicalCloses.map(toGainPct),
    ...Array(forecastLen).fill(null),
  ]

  // Helper: forecast path as % gain, bridged from last historical point
  const makeForecastData = (path: number[] | null): (number | null)[] => {
    if (!path) return Array(histLen + forecastLen + 1).fill(null)
    const bridge: (number | null)[] = Array(histLen - 1).fill(null)
    bridge.push(toGainPct(historicalCloses[histLen - 1]))
    const forecast = path.slice(0, forecastLen).map(toGainPct)
    return [...bridge, ...forecast, ...Array(Math.max(0, forecastLen - forecast.length)).fill(null)]
  }

  const datasets = [
    {
      label: 'Historical',
      data: histData,
      borderColor: 'rgb(148, 163, 184)',
      backgroundColor: 'rgba(148, 163, 184, 0.05)',
      tension: 0.3,
      pointRadius: 0,
    },
    ...(lstmPath
      ? [{
          label: 'LSTM',
          data: makeForecastData(lstmPath),
          borderColor: 'rgb(59, 130, 246)',
          backgroundColor: 'rgba(59, 130, 246, 0.05)',
          tension: 0.3,
          pointRadius: 0,
        }]
      : []),
    ...(xgbPath
      ? [{
          label: 'XGBoost',
          data: makeForecastData(xgbPath),
          borderColor: 'rgb(249, 115, 22)',
          backgroundColor: 'rgba(249, 115, 22, 0.05)',
          tension: 0.3,
          pointRadius: 0,
        }]
      : []),
    ...(transformerPath
      ? [{
          label: 'Transformer',
          data: makeForecastData(transformerPath),
          borderColor: 'rgb(168, 85, 247)',
          backgroundColor: 'rgba(168, 85, 247, 0.05)',
          tension: 0.3,
          pointRadius: 0,
        }]
      : []),
  ]

  const chartData = { labels, datasets }

  const options = {
    responsive: true,
    plugins: {
      legend: { display: true, position: 'top' as const },
      title: {
        display: true,
        text: '2-Year Price Forecast by Model',
      },
      tooltip: {
        callbacks: {
          label: (ctx: any) => `${ctx.dataset.label}: ${ctx.parsed.y >= 0 ? '+' : ''}${ctx.parsed.y.toFixed(1)}%`,
        },
      },
      todayLine: {},
    },
    scales: {
      x: {
        ticks: { maxTicksLimit: 12 },
        title: { display: true, text: 'Trading Days from Today' },
      },
      y: {
        ticks: {
          maxTicksLimit: 8,
          callback: (value: any) => `${value >= 0 ? '+' : ''}${Number(value).toFixed(0)}%`,
        },
        title: { display: true, text: '% Gain from Entry' },
      },
    },
  }

  // Attach todayIndex to chart instance via plugin options trick
  const plugins = [
    {
      ...todayLinePlugin,
      afterDraw(chart: any) {
        const ctx = chart.ctx
        const xAxis = chart.scales.x
        const yAxis = chart.scales.y
        const todayIndex = histLen - 1
        if (todayIndex >= xAxis.min && todayIndex <= xAxis.max) {
          const x = xAxis.getPixelForValue(todayIndex)
          ctx.save()
          ctx.beginPath()
          ctx.moveTo(x, yAxis.top)
          ctx.lineTo(x, yAxis.bottom)
          ctx.lineWidth = 1.5
          ctx.strokeStyle = 'rgba(100,116,139,0.5)'
          ctx.setLineDash([4, 4])
          ctx.stroke()
          ctx.restore()
        }
      },
    },
  ]

  return (
    <div>
      <div className="h-64">
        <Line data={chartData} options={{ ...options, maintainAspectRatio: false }} plugins={plugins} />
      </div>
    </div>
  )
}
