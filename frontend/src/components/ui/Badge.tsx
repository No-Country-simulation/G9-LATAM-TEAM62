import { financialProfileLabel, type FinancialProfile } from '../../lib/api/auth'

interface BadgeProps {
  profile: FinancialProfile
  className?: string
}

const badgeClasses: Record<FinancialProfile, string> = {
  SAVER: 'bg-accent-soft text-accent-ink',
  BALANCED: 'bg-accent-soft text-accent-ink',
  SPENDER: 'bg-warning-soft text-[#7A4B0C]',
  AT_RISK: 'bg-risk-soft text-[#8A1418]',
}

const dotClasses: Record<FinancialProfile, string> = {
  SAVER: 'bg-accent',
  BALANCED: 'bg-accent',
  SPENDER: 'bg-warning',
  AT_RISK: 'bg-risk',
}

export function Badge({ profile, className = '' }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-2 rounded-full px-3.5 py-1.5 text-[13px] font-semibold ${badgeClasses[profile]} ${className}`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${dotClasses[profile]}`} />
      {financialProfileLabel(profile)}
    </span>
  )
}
