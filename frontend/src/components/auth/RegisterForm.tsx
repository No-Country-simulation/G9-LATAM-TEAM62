import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/useAuth'
import { ApiError } from '../../lib/api/client'
import { registerSchema, type RegisterFormValues } from '../../lib/validation/authSchemas'
import { Button } from '../ui/Button'
import { PasswordField } from '../ui/PasswordField'
import { TextField } from '../ui/TextField'

export function RegisterForm() {
  const { register: registerUser } = useAuth()
  const navigate = useNavigate()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema) })

  async function onSubmit(values: RegisterFormValues) {
    setFormError(null)
    try {
      await registerUser(values)
      navigate('/dashboard')
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo crear la cuenta. Intentá nuevamente.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate>
      {formError && (
        <p className="mb-4 rounded-lg bg-risk/10 px-3 py-2 text-sm font-medium text-risk">{formError}</p>
      )}
      <TextField
        label="Nombre completo"
        placeholder="Ej: Julieta Gómez"
        error={errors.name?.message}
        {...register('name')}
      />
      <TextField
        label="Correo electrónico"
        type="email"
        placeholder="tucorreo@email.com"
        error={errors.email?.message}
        {...register('email')}
      />
      <PasswordField
        label="Contraseña"
        placeholder="Mínimo 8, con mayúscula y número"
        error={errors.password?.message}
        {...register('password')}
      />
      <PasswordField
        label="Confirmar contraseña"
        placeholder="Repetí tu contraseña"
        error={errors.confirmPassword?.message}
        {...register('confirmPassword')}
      />
      <div className="mt-2 max-lg:fixed max-lg:inset-x-0 max-lg:bottom-0 max-lg:z-10 max-lg:mt-0 max-lg:border-t max-lg:border-border max-lg:bg-surface max-lg:px-6 max-lg:py-4">
        <Button type="submit" isLoading={isSubmitting}>
          Crear cuenta
        </Button>
      </div>
    </form>
  )
}
