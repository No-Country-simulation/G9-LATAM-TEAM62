import { Link } from 'react-router-dom'
import { RegisterForm } from '../../components/auth/RegisterForm'

function RegisterPage() {
  return (
    <div>
      <h1 className="font-display text-2xl font-bold tracking-tight text-ink">
        Creá tu cuenta
      </h1>
      <p className="mt-1.5 text-[15px] leading-relaxed text-ink-soft">
        Tus datos financieros son privados y solo se usan para calcular tu puntaje.
      </p>
      <div className="mt-5">
        <RegisterForm />
      </div>
      <p className="mt-3 text-center text-sm text-ink-soft">
        ¿Ya tenés cuenta?{' '}
        <Link to="/login" className="font-semibold text-navy">
          Iniciá sesión
        </Link>
      </p>
    </div>
  )
}

export default RegisterPage
