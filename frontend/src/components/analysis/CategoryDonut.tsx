import type { CategoryBreakdown } from '../../context/analysis-context'

const PALETTE = ['#101B33', '#16B892', '#E2963B', '#6E7BF2', '#E1484D', '#8AA0B8']

interface CategoryDonutProps {
  categories: CategoryBreakdown[]
}

export function CategoryDonut({ categories }: CategoryDonutProps) {
  const total = categories.reduce((sum, c) => sum + c.amount, 0)
  let acc = 0
  const stops = categories.map((c, i) => {
    const start = total ? (acc / total) * 100 : 0
    acc += c.amount
    const end = total ? (acc / total) * 100 : 0
    return `${PALETTE[i % PALETTE.length]} ${start}% ${end}%`
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
        {categories.map((c, i) => (
          <div key={c.name} className="flex items-center gap-2.5 py-2">
            <span
              className="h-2.5 w-2.5 shrink-0 rounded-[3px]"
              style={{ backgroundColor: PALETTE[i % PALETTE.length] }}
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
