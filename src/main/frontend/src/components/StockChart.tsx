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

interface OhlcvPoint { date: string; open: number; high: number; low: number; close: number; volume: number }

interface Props { data: OhlcvPoint[]; symbol: string }

export default function StockChart({ data, symbol }: Props) {
  const labels = data.map(d => d.date)
  const closes = data.map(d => d.close)

  const chartData = {
    labels,
    datasets: [{
      label: `${symbol} Close`,
      data: closes,
      borderColor: 'rgb(59, 130, 246)',
      backgroundColor: 'rgba(59, 130, 246, 0.05)',
      tension: 0.1,
      pointRadius: 0,
    }]
  }

  const options = {
    responsive: true,
    plugins: {
      legend: { display: false },
      title: { display: true, text: `${symbol} Price History` }
    },
    scales: { x: { ticks: { maxTicksLimit: 10 } } }
  }

  return (
    <div className="bg-white rounded-lg shadow p-4">
      <Line data={chartData} options={options} />
    </div>
  )
}
