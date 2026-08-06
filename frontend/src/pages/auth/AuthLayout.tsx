import { Link, Outlet, useLocation } from 'react-router-dom'

function AuthLayout() {
  const location = useLocation()
  const isLogin = location.pathname === '/login'

  return (
    <div className="min-h-screen bg-surface lg:flex lg:items-center lg:justify-center lg:overflow-y-auto lg:bg-bg lg:py-4">
      <div className="mx-auto w-full max-w-[440px] px-6 pt-8 pb-28 lg:rounded-3xl lg:border lg:border-border lg:bg-surface lg:px-7 lg:pt-6 lg:pb-6 lg:shadow-[0_20px_44px_-14px_rgba(16,27,51,.12)]">
        <Link
          to="/"
          className="mb-6 inline-flex items-center gap-2 font-display text-base font-semibold text-ink lg:mb-4"
        >
          <ArrowLeftIcon />
          <span className="h-2.5 w-2.5 rounded-full bg-accent" />
          Finance AI
        </Link>

        <div className="mb-5 flex rounded-full bg-surface-alt p-1">
          <Link
            to="/login"
            className={`flex-1 rounded-full py-2.5 text-center text-sm font-semibold transition ${
              isLogin ? 'bg-surface text-ink shadow-sm' : 'text-ink-soft'
            }`}
          >
            Iniciar sesión
          </Link>
          <Link
            to="/register"
            className={`flex-1 rounded-full py-2.5 text-center text-sm font-semibold transition ${
              !isLogin ? 'bg-surface text-ink shadow-sm' : 'text-ink-soft'
            }`}
          >
            Registrarme
          </Link>
        </div>

        <Outlet />
      </div>
    </div>
  )
}

export default AuthLayout

function ArrowLeftIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M19 12H5M11 18l-6-6 6-6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
