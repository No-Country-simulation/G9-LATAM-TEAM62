import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/useAuth'

export function AppHeader() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  if (!user) return null

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <header className="hidden border-b border-border px-8 py-4 lg:flex lg:items-center lg:justify-between">
      <Link to="/dashboard" className="flex items-center gap-2 font-display text-[15px] font-semibold text-ink">
        <span className="h-2.5 w-2.5 rounded-full bg-accent" />
        Finance AI
      </Link>
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2.5 text-[13px] font-medium text-ink-soft">
          <span className="flex h-[30px] w-[30px] items-center justify-center rounded-full bg-navy font-mono text-xs font-semibold text-white">
            {user.name.charAt(0).toUpperCase()}
          </span>
          {user.email}
        </div>
        <button
          type="button"
          onClick={handleLogout}
          className="text-[13px] font-semibold text-ink-soft transition hover:text-ink"
        >
          Cerrar sesión
        </button>
      </div>
    </header>
  )
}
