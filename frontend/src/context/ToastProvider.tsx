import { useCallback, useRef, useState, type ReactNode } from 'react'
import { ToastContext, type ToastType } from './toast-context'

interface ToastItem {
  id: number
  message: string
  type: ToastType
}

const AUTO_DISMISS_MS = 3500

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const nextId = useRef(0)

  const showToast = useCallback((message: string, type: ToastType = 'success') => {
    const id = nextId.current++
    setToasts((prev) => [...prev, { id, message, type }])
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, AUTO_DISMISS_MS)
  }, [])

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 top-[76px] z-50 flex flex-col items-center gap-2 px-4 lg:inset-x-auto lg:right-6 lg:items-end">
        {toasts.map((t) => (
          <div
            key={t.id}
            role="status"
            className="pointer-events-auto flex w-full max-w-sm items-center gap-3 rounded-2xl border border-border bg-surface px-4 py-3 shadow-[0_16px_32px_-14px_rgba(16,27,51,.22)]"
          >
            <span
              className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full ${
                t.type === 'error' ? 'bg-risk-soft text-risk' : 'bg-accent-soft text-accent-ink'
              }`}
            >
              {t.type === 'error' ? <ErrorIcon /> : <CheckIcon />}
            </span>
            <p className="text-sm font-medium text-ink">{t.message}</p>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

function CheckIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4">
      <path d="m5 13 4 4L19 7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function ErrorIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4">
      <path d="M6 6l12 12M18 6 6 18" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
