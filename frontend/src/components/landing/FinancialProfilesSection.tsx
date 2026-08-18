import type { FinancialProfile } from '../../lib/api/auth'
import { Badge } from '../ui/Badge'
import { Reveal } from '../ui/Reveal'

const profiles: { profile: FinancialProfile; description: string }[] = [
  {
    profile: 'SAVER',
    description: 'Ahorrás con regularidad y tus gastos están bajo control.',
  },
  {
    profile: 'BALANCED',
    description: 'Tus ingresos cubren tus gastos, con margen para ahorrar más.',
  },
  {
    profile: 'SPENDER',
    description: 'Gastás más de lo que planificás — hay ajustes simples para mejorar tu margen.',
  },
  {
    profile: 'AT_RISK',
    description: 'Tus gastos presionan tu ingreso — necesitás un plan concreto para aliviarlo.',
  },
]

export function FinancialProfilesSection() {
  return (
    <section className="bg-surface">
      <div className="mx-auto max-w-[1160px] px-6 py-16 md:py-24">
        <Reveal className="max-w-[52ch]">
          <span className="mb-3 inline-flex rounded-full bg-surface-alt px-3 py-1.5 text-xs font-semibold tracking-wide text-ink-soft uppercase">
            Qué significa tu perfil
          </span>
          <h2 className="font-display text-3xl font-bold tracking-tight md:text-4xl">
            Tu forma de gastar, en un perfil claro.
          </h2>
        </Reveal>

        <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {profiles.map((item, index) => (
            <Reveal key={item.profile} delayMs={index * 100}>
              <div className="h-full rounded-2xl border border-border bg-surface p-6 transition duration-200 hover:-translate-y-1 hover:shadow-[0_20px_44px_-18px_rgba(16,27,51,.18)]">
                <Badge profile={item.profile} />
                <p className="mt-4 text-[14.5px] leading-relaxed text-ink-soft">{item.description}</p>
              </div>
            </Reveal>
          ))}
        </div>

        <p className="mt-8 text-[13px] text-ink-faint">
          Cada perfil viene con un porcentaje de confianza: cuantas más transacciones cargues, más preciso es.
        </p>
      </div>
    </section>
  )
}
