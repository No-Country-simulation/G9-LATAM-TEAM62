import { useEffect, useRef, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../../context/useAuth'

interface NavItem {
  to: string
  label: string
}

interface MobileMenuProps {
  items: NavItem[]
}

export function MobileMenu({ items }: MobileMenuProps) {
  const { logout } = useAuth()
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
        aria-label="Abrir menú"
        className="flex h-9 w-9 items-center justify-center rounded-full border border-border text-ink transition hover:border-navy"
      >
        {isOpen ? <CloseIcon /> : <HamburgerIcon />}
      </button>

      {isOpen && (
        <div
          role="menu"
          className="absolute top-full right-0 z-30 mt-2 w-52 overflow-hidden rounded-2xl border border-border bg-surface py-1.5 shadow-[0_20px_44px_-14px_rgba(16,27,51,.22)]"
        >
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              role="menuitem"
              onClick={() => setIsOpen(false)}
              className={({ isActive }) =>
                `block px-4 py-2.5 text-[13px] font-medium transition ${
                  isActive ? 'bg-surface-alt text-ink' : 'text-ink hover:bg-surface-alt'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
          <div className="my-1.5 border-t border-border" />
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

function HamburgerIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M4 7h16M4 12h16M4 17h16" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function CloseIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M6 6l12 12M18 6 6 18" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
