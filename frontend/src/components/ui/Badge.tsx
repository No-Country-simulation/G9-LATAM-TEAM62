import type { FinancialProfile } from '../../context/analysis-context'

interface BadgeProps {
  profile: FinancialProfile
  className?: string
}

const badgeClasses: Record<FinancialProfile, string> = {
  Saludable: 'bg-accent-soft text-accent-ink',
  'Necesita atención': 'bg-warning-soft text-[#7A4B0C]',
  'En riesgo': 'bg-risk-soft text-[#8A1418]',
}

const dotClasses: Record<FinancialProfile, string> = {
  Saludable: 'bg-accent',
  'Necesita atención': 'bg-warning',
  'En riesgo': 'bg-risk',
}

export function Badge({ profile, className = '' }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full px-3.5 py-1.5 text-[13px] font-semibold ${badgeClasses[profile]} ${className}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${dotClasses[profile]}`} />
      {profile}
    </span>
  )
}
