import { useEffect, useRef, useState } from 'react'
import { categoryColor, expenseCategoryOptions, type ExpenseCategory } from '../../lib/api/transactions'

interface CategorySelectProps {
  value: string
  onChange: (value: ExpenseCategory) => void
  onBlur?: () => void
  disabled?: boolean
  placeholder?: string
  className?: string
}

export function CategorySelect({
  value,
  onChange,
  onBlur,
  disabled = false,
  placeholder = 'Categoría',
  className = '',
}: CategorySelectProps) {
  const [isOpen, setIsOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!isOpen) return

    function handleClickOutside(event: MouseEvent) {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setIsOpen(false)
        onBlur?.()
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') setIsOpen(false)
    }

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleEscape)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen])

  const selected = expenseCategoryOptions.find((o) => o.value === value)

  function handleSelect(next: ExpenseCategory) {
    onChange(next)
    setIsOpen(false)
    onBlur?.()
  }

  return (
    <div ref={rootRef} className={`relative ${className}`}>
      <button
        type="button"
        onClick={() => setIsOpen((v) => !v)}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        style={{ borderLeftColor: selected ? categoryColor(selected.value) : 'var(--color-border)' }}
        className="flex w-full items-center gap-1.5 rounded-lg border-[1.5px] border-l-[4px] border-border bg-transparent px-2.5 py-1.5 text-xs font-medium text-ink-soft outline-none transition focus:border-navy disabled:opacity-60"
      >
        <span className="min-w-0 flex-1 truncate text-left">{selected ? selected.label : placeholder}</span>
        <ChevronDownIcon className={`shrink-0 transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </button>

      {isOpen && (
        <div
          role="listbox"
          className="absolute top-full left-0 z-20 mt-1.5 max-h-64 w-full min-w-[190px] overflow-y-auto rounded-xl border border-border bg-surface py-1.5 shadow-[0_20px_44px_-14px_rgba(16,27,51,.22)]"
        >
          {expenseCategoryOptions.map((option) => (
            <button
              key={option.value}
              type="button"
              role="option"
              aria-selected={option.value === value}
              onClick={() => handleSelect(option.value)}
              style={{ borderLeftColor: categoryColor(option.value) }}
              className={`flex w-full items-center border-l-[4px] py-2 pr-3 pl-2.5 text-left text-xs font-medium transition hover:bg-surface-alt ${
                option.value === value ? 'text-ink' : 'text-ink-soft'
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

function ChevronDownIcon({ className = '' }: { className?: string }) {
  return (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" className={className}>
      <path d="m6 9 6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
