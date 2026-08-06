import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/useAuth'
import { loginSchema, type LoginFormValues } from '../../lib/validation/authSchemas'
import { Button } from '../ui/Button'
import { PasswordField } from '../ui/PasswordField'
import { TextField } from '../ui/TextField'

export function LoginForm() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) })

  async function onSubmit(values: LoginFormValues) {
    await login(values)
    navigate('/dashboard')
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate>
      <TextField
        label="Correo electrónico"
        type="email"
        placeholder="tucorreo@email.com"
        error={errors.email?.message}
        {...register('email')}
      />
      <PasswordField
        label="Contraseña"
        placeholder="Mínimo 8 caracteres"
        error={errors.password?.message}
        {...register('password')}
      />
      <div className="mt-2 max-lg:fixed max-lg:inset-x-0 max-lg:bottom-0 max-lg:z-10 max-lg:mt-0 max-lg:border-t max-lg:border-border max-lg:bg-surface max-lg:px-6 max-lg:py-4">
        <Button type="submit" isLoading={isSubmitting}>
          Continuar
        </Button>
      </div>
    </form>
  )
}
