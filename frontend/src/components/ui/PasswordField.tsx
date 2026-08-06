import { forwardRef, useState, type InputHTMLAttributes } from 'react'

interface PasswordFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
}

export const PasswordField = forwardRef<HTMLInputElement, PasswordFieldProps>(
  ({ label, error, id, className = '', ...props }, ref) => {
    const [visible, setVisible] = useState(false)
    const inputId = id ?? props.name

    return (
      <div className="mb-3">
        <label htmlFor={inputId} className="mb-1 block text-[13px] font-semibold text-ink-soft">
          {label}
        </label>
        <div className="relative">
          <input
            id={inputId}
            ref={ref}
            type={visible ? 'text' : 'password'}
            className={`w-full rounded-xl border-[1.5px] px-4 py-3 pr-12 text-[15px] text-ink outline-none transition focus:border-navy ${
              error ? 'border-risk' : 'border-border'
            } ${className}`}
            aria-invalid={!!error}
            aria-describedby={error ? `${inputId}-error` : undefined}
            {...props}
          />
          <button
            type="button"
            onClick={() => setVisible((v) => !v)}
            className="absolute top-1/2 right-3 -translate-y-1/2 text-ink-faint hover:text-ink-soft"
            aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            tabIndex={-1}
          >
            {visible ? <EyeOffIcon /> : <EyeIcon />}
          </button>
        </div>
        {error && (
          <p id={`${inputId}-error`} className="mt-1.5 text-xs font-medium text-risk">
            {error}
          </p>
        )}
      </div>
    )
  },
)
PasswordField.displayName = 'PasswordField'

function EyeIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7Z" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="12" cy="12" r="3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function EyeOffIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M3 3l18 18" strokeLinecap="round" />
      <path
        d="M10.6 5.1A11.6 11.6 0 0 1 12 5c7 0 11 7 11 7a13.7 13.7 0 0 1-3.2 3.9M6.6 6.6C3.9 8.3 2 12 2 12s4 7 11 7a10.4 10.4 0 0 0 5-1.3"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
