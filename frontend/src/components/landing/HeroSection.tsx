import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useInView } from '../../hooks/useInView'
import { ScoreGauge } from '../ui/ScoreGauge'

const FINAL_SCORE = 74
const COUNT_DURATION_MS = 1400

export function HeroSection() {
  const { ref, inView } = useInView<HTMLDivElement>({ threshold: 0.4 })
  const [score, setScore] = useState(0)

  useEffect(() => {
    if (!inView) return

    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      setScore(FINAL_SCORE)
      return
    }

    const start = performance.now()
    let frame: number

    function tick(now: number) {
      const progress = Math.min((now - start) / COUNT_DURATION_MS, 1)
      const eased = 1 - (1 - progress) ** 3
      setScore(Math.round(eased * FINAL_SCORE))
      if (progress < 1) frame = requestAnimationFrame(tick)
    }

    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
  }, [inView])

  return (
    <div className="mx-auto flex max-w-[1160px] flex-col px-6 pt-8 md:pt-14">
      <nav className="flex items-center justify-between pb-8">
        <div className="flex items-center gap-2 font-semibold">
          <span className="h-2.5 w-2.5 rounded-full bg-accent" />
          Finance AI
        </div>
      </nav>

      <div ref={ref} className="grid flex-1 items-center gap-14 pb-20 md:grid-cols-[1.05fr_0.95fr]">
        <div>
          <span className="mb-4 inline-flex rounded-full bg-accent-soft px-3 py-1.5 text-xs font-semibold tracking-wide text-accent-ink uppercase">
            Salud financiera con IA
          </span>
          <h1 className="text-4xl leading-tight font-bold tracking-tight md:text-6xl">
            El estado real de tus finanzas,{' '}
            <em className="not-italic bg-[linear-gradient(180deg,transparent_60%,#E1F7F0_60%)]">
              en un solo número.
            </em>
          </h1>
          <p className="mt-5 max-w-[44ch] text-base text-ink-soft md:text-lg">
            Contanos tus ingresos y tus gastos. Finance AI analiza tu situación y te dice qué ajustar para mejorar.
          </p>
          <div className="mt-8 flex flex-col gap-3 sm:flex-row">
            <Link
              to="/register"
              className="rounded-full bg-accent px-6 py-4 text-center font-semibold text-[#04231C] transition hover:shadow-lg"
            >
              Comenzar análisis
            </Link>
            <Link
              to="/login"
              className="rounded-full border border-border px-6 py-4 text-center font-semibold text-navy transition hover:border-navy"
            >
              Iniciar sesión
            </Link>
          </div>
        </div>

        <div className="flex items-center justify-center">
          <div className="w-full max-w-[300px] rounded-[26px] bg-navy p-9 text-center shadow-[0_20px_44px_-14px_rgba(16,27,51,.22)]">
            <ScoreGauge score={score} size={140} stroke={11} />
            <p className="mt-4 text-xs leading-relaxed text-white/55">
              Puntaje calculado a partir de tus ingresos, deudas y hábitos de ahorro.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
