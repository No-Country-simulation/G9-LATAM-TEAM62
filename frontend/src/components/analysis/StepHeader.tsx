interface StepHeaderProps {
  title: string
  onBack: () => void
  step?: number
  totalSteps?: number
}

export function StepHeader({ title, onBack, step, totalSteps }: StepHeaderProps) {
  const showProgress = step != null && totalSteps != null
  const pct = showProgress ? (step / totalSteps) * 100 : 0

  return (
    <div>
      <div className="flex items-center gap-3 px-6 pt-6 lg:px-0 lg:pt-0">
        <button
          type="button"
          onClick={onBack}
          aria-label="Volver"
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-border text-ink transition hover:border-navy"
        >
          <ArrowLeftIcon />
        </button>
        <h1 className="font-display text-[17px] font-semibold">{title}</h1>
      </div>
      {showProgress && (
        <>
          <p className="px-6 pt-3 text-xs font-semibold text-ink-soft lg:px-0">
            Paso {step} de {totalSteps}
          </p>
          <div className="mx-6 mt-1.5 h-1 overflow-hidden rounded-full bg-surface-alt lg:mx-0">
            <div
              className="h-full rounded-full bg-navy transition-[width] duration-300"
              style={{ width: `${pct}%` }}
            />
          </div>
        </>
      )}
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
