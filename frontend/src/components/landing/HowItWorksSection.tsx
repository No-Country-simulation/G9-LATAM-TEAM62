import { Badge } from '../ui/Badge'
import { NavyPanel } from '../ui/NavyPanel'
import { Reveal } from '../ui/Reveal'
import { ScoreGauge } from '../ui/ScoreGauge'

const steps = [
  {
    number: '01',
    title: 'Cargás tus transacciones',
    description: 'A mano, una por una, o subiendo tu cartola bancaria en Excel, CSV o PDF — vos elegís.',
    score: 0,
    scoreLabel: 'Cargando',
  },
  {
    number: '02',
    title: 'La IA clasifica cada gasto',
    description: 'Detecta la categoría y el método de pago de cada movimiento, sin que tengas que hacerlo vos.',
    score: 45,
    scoreLabel: 'Clasificando',
  },
  {
    number: '03',
    title: 'Recibís tu perfil y recomendaciones',
    description:
      'Tu perfil financiero, el desglose de tus gastos por categoría y recomendaciones concretas para mejorar.',
    score: 82,
    scoreLabel: 'Confianza del perfil',
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
          De tus movimientos a tu perfil, en tres pasos.
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
                <NavyPanel className="text-center">
                  <ScoreGauge score={step.score} size={116} stroke={9} label={step.scoreLabel} />
                  {index === 2 && (
                    <div className="mt-4">
                      <Badge profile="BALANCED" />
                    </div>
                  )}
                </NavyPanel>
              </div>
            </div>
          </Reveal>
        ))}
      </div>
    </section>
  )
}
