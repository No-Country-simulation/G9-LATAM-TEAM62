import { useLayoutEffect, useRef, useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/useAuth'
import { Logo } from '../ui/Logo'
import { AccountMenu } from './AccountMenu'
import { MobileMenu } from './MobileMenu'

const navItems = [
  { to: '/dashboard', label: 'Inicio' },
  { to: '/transactions', label: 'Transacciones' },
  { to: '/recommendations', label: 'Recomendaciones' },
  { to: '/profile', label: 'Perfil' },
]

export function AppHeader() {
  const { user } = useAuth()
  const location = useLocation()
  const navRef = useRef<HTMLElement>(null)
  const [indicator, setIndicator] = useState<{ left: number; width: number } | null>(null)

  useLayoutEffect(() => {
    function measure() {
      const activeLink = navRef.current?.querySelector<HTMLAnchorElement>('a[aria-current="page"]')
      setIndicator(activeLink ? { left: activeLink.offsetLeft, width: activeLink.offsetWidth } : null)
    }

    measure()
    window.addEventListener('resize', measure)
    return () => window.removeEventListener('resize', measure)
  }, [location.pathname])

  if (!user) return null

  return (
    <header className="flex h-16 items-center justify-between border-b border-border px-6 lg:px-8">
      <Link to="/dashboard" className="flex items-center gap-2 font-display text-[15px] font-semibold text-ink">
        <Logo />
        Finance AI
      </Link>

      <nav ref={navRef} className="relative hidden items-center gap-1 lg:flex">
        {indicator && (
          <span
            className="absolute top-0 bottom-0 z-0 rounded-full bg-surface-alt transition-[left,width] duration-300 ease-out"
            style={{ left: indicator.left, width: indicator.width }}
            aria-hidden
          />
        )}
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `relative z-10 rounded-full px-4 py-2 text-[13px] font-semibold transition-colors duration-300 ${
                isActive ? 'text-ink' : 'text-ink-soft hover:text-ink'
              }`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="hidden lg:block">
        <AccountMenu />
      </div>

      <div className="lg:hidden">
        <MobileMenu items={navItems} />
      </div>
    </header>
  )
}
