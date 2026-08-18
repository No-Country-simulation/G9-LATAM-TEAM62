import { useEffect, useState } from 'react'

interface ScoreGaugeProps {
  score: number
  size?: number
  stroke?: number
  trackColor?: string
  progressColor?: string
  scoreClassName?: string
  labelClassName?: string
  label?: string
  glow?: boolean
  animate?: boolean
  animateDurationMs?: number
}

export function ScoreGauge({
  score,
  size = 140,
  stroke = 11,
  trackColor = 'rgba(255,255,255,.14)',
  progressColor = '#16B892',
  scoreClassName = 'text-white',
  labelClassName = 'text-white/60',
  label = 'Puntaje de salud',
  glow = false,
  animate = false,
  animateDurationMs = 1200,
}: ScoreGaugeProps) {
  const [displayed, setDisplayed] = useState(animate ? 0 : score)

  useEffect(() => {
    if (!animate) {
      setDisplayed(score)
      return
    }
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      setDisplayed(score)
      return
    }

    const start = performance.now()
    let frame: number

    function tick(now: number) {
      const progress = Math.min((now - start) / animateDurationMs, 1)
      const eased = 1 - (1 - progress) ** 3
      setDisplayed(eased * score)
      if (progress < 1) frame = requestAnimationFrame(tick)
    }

    frame = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(frame)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [animate, score])

  const radius = (size - stroke) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference * (1 - displayed / 100)

  return (
    <div className="relative mx-auto" style={{ width: size, height: size }}>
      {glow && (
        <div
          className="absolute inset-[-22%] -z-10 rounded-full opacity-70 blur-2xl"
          style={{ background: `radial-gradient(circle, ${progressColor}55 0%, transparent 68%)` }}
          aria-hidden
        />
      )}
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke={trackColor} strokeWidth={stroke} />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={progressColor}
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className={`font-mono text-4xl font-semibold ${scoreClassName}`}>{Math.round(displayed)}</span>
        <span className={`mt-1 text-[11px] font-semibold tracking-wide uppercase ${labelClassName}`}>{label}</span>
      </div>
    </div>
  )
}
