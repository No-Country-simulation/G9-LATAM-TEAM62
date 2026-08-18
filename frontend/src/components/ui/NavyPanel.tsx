import type { ReactNode } from 'react'

interface NavyPanelProps {
  children: ReactNode
  className?: string
}

export function NavyPanel({ children, className = '' }: NavyPanelProps) {
  return (
    <div
      className={`relative overflow-hidden rounded-[28px] bg-navy px-6 pt-8 pb-7 text-white shadow-[0_24px_54px_-18px_rgba(16,27,51,.35)] ${className}`}
    >
      <div
        className="pointer-events-none absolute inset-0 opacity-[0.07]"
        style={{
          backgroundImage:
            'linear-gradient(rgba(255,255,255,.6) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,.6) 1px, transparent 1px)',
          backgroundSize: '28px 28px',
        }}
        aria-hidden
      />
      <div className="relative">{children}</div>
    </div>
  )
}
