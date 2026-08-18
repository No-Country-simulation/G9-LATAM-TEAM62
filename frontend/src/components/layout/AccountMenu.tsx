import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../../context/useAuth'

export function AccountMenu() {
  const { user, logout } = useAuth()
  const [isOpen, setIsOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!isOpen) return

    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false)
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
  }, [isOpen])

  if (!user) return null

  function handleLogout() {
    setIsOpen(false)
    logout()
  }

  return (
    <div ref={menuRef} className="relative">
      <button
        type="button"
        onClick={() => setIsOpen((v) => !v)}
        aria-expanded={isOpen}
        aria-haspopup="menu"
        className="flex items-center gap-2.5 rounded-full py-1 pr-1 pl-1 text-[13px] font-medium text-ink-soft transition hover:text-ink"
      >
        <span className="flex h-[30px] w-[30px] items-center justify-center rounded-full bg-navy font-mono text-xs font-semibold text-white">
          {user.name.charAt(0).toUpperCase()}
        </span>
        {user.name}
        <ChevronDownIcon className={`transition-transform ${isOpen ? 'rotate-180' : ''}`} />
      </button>

      {isOpen && (
        <div
          role="menu"
          className="absolute top-full right-0 z-30 mt-2 w-48 overflow-hidden rounded-2xl border border-border bg-surface py-1.5 shadow-[0_20px_44px_-14px_rgba(16,27,51,.22)]"
        >
          <button
            type="button"
            role="menuitem"
            onClick={handleLogout}
            className="block w-full px-4 py-2.5 text-left text-[13px] font-semibold text-risk transition hover:bg-risk-soft"
          >
            Cerrar sesión
          </button>
        </div>
      )}
    </div>
  )
}

function ChevronDownIcon({ className = '' }: { className?: string }) {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" className={className}>
      <path d="m6 9 6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
