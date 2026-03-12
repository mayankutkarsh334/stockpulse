import { FilterCriteria } from '../api/screenerApi'

interface Props {
  filters: FilterCriteria[]
  onChange: (f: FilterCriteria[]) => void
  indicators: string[]
}

export default function FilterBuilder({ filters, onChange, indicators }: Props) {
  const addFilter = () => onChange([...filters, { indicator: indicators[0], operator: 'GT', value: 0 }])
  const removeFilter = (i: number) => onChange(filters.filter((_, idx) => idx !== i))
  const updateFilter = (i: number, updates: Partial<FilterCriteria>) => {
    onChange(filters.map((f, idx) => idx === i ? { ...f, ...updates } : f))
  }

  return (
    <div>
      <div className="text-sm font-medium mb-2">Filters</div>
      {filters.map((f, i) => (
        <div key={i} className="flex gap-2 mb-2 items-center">
          <select className="border rounded px-2 py-1.5 text-sm"
            value={f.indicator} onChange={e => updateFilter(i, { indicator: e.target.value })}>
            {indicators.map(ind => <option key={ind}>{ind}</option>)}
          </select>
          <select className="border rounded px-2 py-1.5 text-sm"
            value={f.operator} onChange={e => updateFilter(i, { operator: e.target.value })}>
            {['GT', 'GTE', 'LT', 'LTE', 'EQ'].map(op => <option key={op}>{op}</option>)}
          </select>
          <input type="number" className="border rounded px-2 py-1.5 text-sm w-28"
            value={f.value} onChange={e => updateFilter(i, { value: Number(e.target.value) })} />
          <button onClick={() => removeFilter(i)}
            className="text-red-500 hover:text-red-700 text-sm px-1">Remove</button>
        </div>
      ))}
      <button onClick={addFilter}
        className="text-sm text-blue-600 hover:underline mt-1">+ Add Filter</button>
    </div>
  )
}
