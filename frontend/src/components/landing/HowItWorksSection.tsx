import { Reveal } from '../ui/Reveal'
import { ScoreGauge } from '../ui/ScoreGauge'

const steps = [
  {
    number: '01',
    title: 'Contás lo básico',
    description: 'Tu ingreso mensual, tu nivel de deuda y con qué frecuencia ahorrás. Menos de un minuto.',
    score: 0,
    scoreLabel: 'Arrancando',
  },
  {
    number: '02',
    title: 'La IA revisa tus movimientos',
    description: 'Categoriza cada gasto y entiende qué es esencial y qué es discrecional en tu día a día.',
    score: 45,
    scoreLabel: 'Analizando',
  },
  {
    number: '03',
    title: 'Recibís tu puntaje y qué hacer',
    description: 'Un número claro de 0 a 100 y tres recomendaciones concretas, en lenguaje simple.',
    score: 82,
    scoreLabel: 'Puntaje de salud',
  },
]

export function HowItWorksSection() {
  return (
    <section className="mx-auto max-w-[1160px] px-6 py-16 md:py-24">
      <Reveal className="max-w-[52ch]">
        <span className="mb-3 inline-flex rounded-full bg-accent-soft px-3 py-1.5 text-xs font-semibold tracking-wide text-accent-ink uppercase">
          Cómo funciona
        </span>
        <h2 className="font-display text-3xl font-bold tracking-tight md:text-4xl">
          De tus números a un plan, en tres pasos.
        </h2>
      </Reveal>

      <div className="mt-14 flex flex-col gap-16 md:gap-20">
        {steps.map((step, index) => (
          <Reveal key={step.number} delayMs={100}>
            <div
              className={`flex flex-col items-center gap-10 md:gap-16 ${
                index % 2 === 1 ? 'md:flex-row-reverse' : 'md:flex-row'
              }`}
            >
              <div className="flex-1">
                <span className="font-mono text-sm font-semibold text-accent-ink">{step.number}</span>
                <h3 className="mt-2 font-display text-2xl font-bold tracking-tight text-ink">{step.title}</h3>
                <p className="mt-3 max-w-[46ch] text-[15px] leading-relaxed text-ink-soft">{step.description}</p>
              </div>
              <div className="flex flex-1 items-center justify-center">
                <div className="rounded-[26px] bg-navy p-8 shadow-[0_20px_44px_-14px_rgba(16,27,51,.22)]">
                  <ScoreGauge score={step.score} size={116} stroke={9} label={step.scoreLabel} />
                </div>
              </div>
            </div>
          </Reveal>
        ))}
      </div>
    </section>
  )
}
