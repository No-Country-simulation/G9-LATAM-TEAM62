import { forwardRef, type InputHTMLAttributes } from 'react'

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(
  ({ label, error, id, className = '', ...props }, ref) => {
    const inputId = id ?? props.name

    return (
      <div className="mb-3">
        <label htmlFor={inputId} className="mb-1 block text-[13px] font-semibold text-ink-soft">
          {label}
        </label>
        <input
          id={inputId}
          ref={ref}
          className={`w-full rounded-xl border-[1.5px] px-4 py-3 text-[15px] text-ink outline-none transition focus:border-navy ${
            error ? 'border-risk' : 'border-border'
          } ${className}`}
          aria-invalid={!!error}
          aria-describedby={error ? `${inputId}-error` : undefined}
          {...props}
        />
        {error && (
          <p id={`${inputId}-error`} className="mt-1.5 text-xs font-medium text-risk">
            {error}
          </p>
        )}
      </div>
    )
  },
)
TextField.displayName = 'TextField'
