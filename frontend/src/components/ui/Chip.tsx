import type { ButtonHTMLAttributes } from 'react'

interface ChipProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  selected?: boolean
}

export function Chip({ selected = false, className = '', children, ...props }: ChipProps) {
  return (
    <button
      type="button"
      className={`rounded-full border-[1.5px] px-4 py-2 text-[13px] font-semibold transition ${
        selected
          ? 'border-navy bg-navy text-white'
          : 'border-border bg-surface text-ink-soft hover:border-navy-soft'
      } ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
