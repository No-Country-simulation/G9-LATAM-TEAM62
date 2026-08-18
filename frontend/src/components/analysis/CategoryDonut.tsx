import { categoryColor } from '../../lib/api/transactions'
import type { CategoryBreakdown } from '../../context/analysis-context'

interface CategoryDonutProps {
  categories: CategoryBreakdown[]
}

export function CategoryDonut({ categories }: CategoryDonutProps) {
  const total = categories.reduce((sum, c) => sum + c.amount, 0)
  let acc = 0
  const stops = categories.map((c) => {
    const start = total ? (acc / total) * 100 : 0
    acc += c.amount
    const end = total ? (acc / total) * 100 : 0
    return `${categoryColor(c.category)} ${start}% ${end}%`
  })

  return (
    <div>
      <div
        className="mx-auto h-[150px] w-[150px] rounded-full"
        style={{
          backgroundImage: `radial-gradient(closest-side, var(--color-surface) 62%, transparent 63% 100%), conic-gradient(${stops.join(', ')})`,
        }}
      />
      <div className="mt-4">
        {categories.map((c) => (
          <div key={c.category} className="flex items-center gap-2.5 py-2">
            <span
              className="h-2.5 w-2.5 shrink-0 rounded-[3px]"
              style={{ backgroundColor: categoryColor(c.category) }}
            />
            <span className="flex-1 text-[13.5px] text-ink">{c.name}</span>
            <span className="font-mono text-[13px] font-semibold text-ink-soft">
              ${Math.round(c.amount).toLocaleString('es-AR')}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
