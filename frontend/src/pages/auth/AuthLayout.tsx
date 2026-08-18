import { Link, Outlet, useLocation } from 'react-router-dom'
import { Logo } from '../../components/ui/Logo'

function AuthLayout() {
  const location = useLocation()
  const isLogin = location.pathname === '/login'

  return (
    <div className="min-h-screen bg-surface lg:flex lg:items-center lg:justify-center lg:overflow-y-auto lg:bg-bg lg:py-4">
      <div className="mx-auto w-full max-w-[440px] px-6 pt-8 pb-28 lg:max-w-[460px] lg:rounded-3xl lg:border lg:border-border lg:bg-surface lg:px-8 lg:pt-6 lg:pb-6 lg:shadow-[0_20px_44px_-14px_rgba(16,27,51,.12)]">
        <Link
          to="/"
          className="mb-6 inline-flex items-center gap-2 font-display text-base font-semibold text-ink lg:mb-4"
        >
          <ArrowLeftIcon />
          <Logo />
          Finance AI
        </Link>

        <div className="relative mb-5 flex rounded-full bg-surface-alt p-1">
          <div
            className={`absolute top-1 bottom-1 left-1 w-[calc(50%-4px)] rounded-full bg-surface shadow-sm transition-transform duration-300 ease-out ${
              isLogin ? 'translate-x-0' : 'translate-x-full'
            }`}
            aria-hidden
          />
          <Link
            to="/login"
            className={`relative z-10 flex-1 rounded-full py-2.5 text-center text-sm font-semibold transition-colors duration-300 ${
              isLogin ? 'text-ink' : 'text-ink-soft'
            }`}
          >
            Iniciar sesión
          </Link>
          <Link
            to="/register"
            className={`relative z-10 flex-1 rounded-full py-2.5 text-center text-sm font-semibold transition-colors duration-300 ${
              !isLogin ? 'text-ink' : 'text-ink-soft'
            }`}
          >
            Registrarme
          </Link>
        </div>

        <div key={location.pathname} className="animate-[fade-in_.3s_ease-out] motion-reduce:animate-none">
          <Outlet />
        </div>
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
