import { useNavigate } from 'react-router-dom'

interface PageHeaderProps {
  title: string
  backTo?: string
}

export function PageHeader({ title, backTo = '/dashboard' }: PageHeaderProps) {
  const navigate = useNavigate()

  return (
    <div className="flex items-center gap-3">
      <button
        type="button"
        onClick={() => navigate(backTo)}
        aria-label="Volver"
        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-border text-ink transition hover:border-navy"
      >
        <ArrowLeftIcon />
      </button>
      <h1 className="font-display text-2xl font-bold text-ink lg:text-[28px]">{title}</h1>
    </div>
  )
}

function ArrowLeftIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M19 12H5M11 18l-6-6 6-6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
