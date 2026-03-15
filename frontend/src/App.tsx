import { HashRouter, Routes, Route, NavLink } from 'react-router-dom'
import PortfolioPage from './pages/PortfolioPage'
import ScreenerPage from './pages/ScreenerPage'
import AnalysisPage from './pages/AnalysisPage'
import ChartPage from './pages/ChartPage'
import CsvAnalysisPage from './pages/CsvAnalysisPage'
import MonthlyStrategyPage from './pages/MonthlyStrategyPage'
import CoffeeCanPage from './pages/CoffeeCanPage'

export default function App() {
  return (
    <HashRouter>
      <div className="min-h-screen flex flex-col">
        <nav className="bg-slate-800 text-white px-6 py-3 flex items-center gap-6">
          <span className="font-bold text-lg mr-4">StockInvest</span>
          <NavLink to="/" end className={({ isActive }) => isActive ? 'text-blue-300' : 'hover:text-blue-300'}>
            Portfolio
          </NavLink>
          <NavLink to="/screener" className={({ isActive }) => isActive ? 'text-blue-300' : 'hover:text-blue-300'}>
            Screener
          </NavLink>
          <NavLink to="/analysis" className={({ isActive }) => isActive ? 'text-blue-300' : 'hover:text-blue-300'}>
            Analysis
          </NavLink>
          <NavLink to="/csv-analysis" className={({ isActive }) => isActive ? 'text-blue-300' : 'hover:text-blue-300'}>
            CSV Analysis
          </NavLink>
          <NavLink to="/strategy" className={({ isActive }) => isActive ? 'text-blue-300' : 'hover:text-blue-300'}>
            Strategy
          </NavLink>
          <NavLink to="/coffee-can" className={({ isActive }) => isActive ? 'text-blue-300' : 'hover:text-blue-300'}>
            Coffee Can
          </NavLink>
        </nav>
        <main className="flex-1 p-6">
          <Routes>
            <Route path="/" element={<PortfolioPage />} />
            <Route path="/screener" element={<ScreenerPage />} />
            <Route path="/analysis" element={<AnalysisPage />} />
            <Route path="/chart/:symbol" element={<ChartPage />} />
            <Route path="/csv-analysis" element={<CsvAnalysisPage />} />
            <Route path="/strategy" element={<MonthlyStrategyPage />} />
            <Route path="/coffee-can" element={<CoffeeCanPage />} />
          </Routes>
        </main>
      </div>
    </HashRouter>
  )
}
