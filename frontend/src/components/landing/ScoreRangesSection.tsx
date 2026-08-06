import { Reveal } from '../ui/Reveal'

const ranges = [
  {
    label: 'Saludable',
    range: '70 – 100',
    dotColor: 'bg-accent',
    badgeClass: 'bg-accent-soft text-accent-ink',
    description: 'Tus ingresos cubren tus gastos con margen y ahorrás con regularidad.',
  },
  {
    label: 'Necesita atención',
    range: '40 – 69',
    dotColor: 'bg-warning',
    badgeClass: 'bg-warning-soft text-[#7A4B0C]',
    description: 'Estás cerca del límite: unos pocos ajustes pueden mejorar tu margen rápido.',
  },
  {
    label: 'En riesgo',
    range: '0 – 39',
    dotColor: 'bg-risk',
    badgeClass: 'bg-risk-soft text-[#8A1418]',
    description: 'Tus gastos y deudas presionan tu ingreso — hay acciones concretas para aliviarlo ahora.',
  },
]

export function ScoreRangesSection() {
  return (
    <section className="bg-surface">
      <div className="mx-auto max-w-[1160px] px-6 py-16 md:py-24">
        <Reveal className="max-w-[52ch]">
          <span className="mb-3 inline-flex rounded-full bg-surface-alt px-3 py-1.5 text-xs font-semibold tracking-wide text-ink-soft uppercase">
            Qué significa tu puntaje
          </span>
          <h2 className="font-display text-3xl font-bold tracking-tight md:text-4xl">
            Un número, tres lecturas posibles.
          </h2>
        </Reveal>

        <div className="mt-12 grid gap-5 md:grid-cols-3">
          {ranges.map((item, index) => (
            <Reveal key={item.label} delayMs={index * 100}>
              <div className="h-full rounded-2xl border border-border bg-surface p-7 transition duration-200 hover:-translate-y-1 hover:shadow-[0_20px_44px_-18px_rgba(16,27,51,.18)]">
                <span
                  className={`inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-semibold ${item.badgeClass}`}
                >
                  <span className={`h-1.5 w-1.5 rounded-full ${item.dotColor}`} />
                  {item.label}
                </span>
                <p className="mt-4 font-mono text-2xl font-semibold text-ink">{item.range}</p>
                <p className="mt-3 text-[14.5px] leading-relaxed text-ink-soft">{item.description}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  )
}
