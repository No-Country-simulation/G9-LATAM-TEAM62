import type { ButtonHTMLAttributes } from 'react'

type ButtonVariant = 'primary' | 'accent' | 'ghost'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  isLoading?: boolean
  fullWidth?: boolean
}

const variantClasses: Record<ButtonVariant, string> = {
  primary: 'bg-navy text-white hover:shadow-[0_10px_24px_-8px_rgba(16,27,51,.45)]',
  accent: 'bg-accent text-[#04231C] hover:shadow-[0_10px_24px_-8px_rgba(22,184,146,.5)]',
  ghost: 'bg-transparent text-navy border border-border hover:border-navy',
}

export function Button({
  variant = 'primary',
  isLoading = false,
  fullWidth = true,
  disabled,
  children,
  className = '',
  ...props
}: ButtonProps) {
  return (
    <button
      className={`flex items-center justify-center gap-2 rounded-full px-5 py-3.5 text-[15px] font-semibold transition active:scale-[0.98] disabled:pointer-events-none disabled:opacity-40 ${
        fullWidth ? 'w-full' : 'w-auto'
      } ${variantClasses[variant]} ${className}`}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? 'Enviando…' : children}
    </button>
  )
}
