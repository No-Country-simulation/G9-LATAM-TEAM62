interface ScoreGaugeProps {
  score: number
  size?: number
  stroke?: number
  trackColor?: string
  progressColor?: string
  scoreClassName?: string
  labelClassName?: string
  label?: string
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
}: ScoreGaugeProps) {
  const radius = (size - stroke) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference * (1 - score / 100)

  return (
    <div className="relative mx-auto" style={{ width: size, height: size }}>
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
        <span className={`font-mono text-4xl font-semibold ${scoreClassName}`}>{Math.round(score)}</span>
        <span className={`mt-1 text-[11px] font-semibold tracking-wide uppercase ${labelClassName}`}>{label}</span>
      </div>
    </div>
  )
}
