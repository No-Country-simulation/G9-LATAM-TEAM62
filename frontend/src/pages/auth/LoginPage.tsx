import { Link } from 'react-router-dom'
import { LoginForm } from '../../components/auth/LoginForm'

function LoginPage() {
  return (
    <div>
      <h1 className="font-display text-2xl font-bold tracking-tight text-ink">
        Bienvenido de nuevo
      </h1>
      <p className="mt-1.5 text-[15px] leading-relaxed text-ink-soft">
        Iniciá sesión para ver tu puntaje de salud financiera más reciente.
      </p>
      <div className="mt-5">
        <LoginForm />
      </div>
      <p className="mt-3 text-center text-sm text-ink-soft">
        ¿No tenés cuenta?{' '}
        <Link to="/register" className="font-semibold text-navy">
          Registrate
        </Link>
      </p>
    </div>
  )
}

export default LoginPage
