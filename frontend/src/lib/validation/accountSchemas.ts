import { z } from 'zod'
import type { SavingFrequency } from '../api/auth'

export const savingFrequencyValues = ['NEVER', 'RARELY', 'MONTHLY', 'BIWEEKLY', 'WEEKLY', 'DAILY'] as const

export const savingFrequencyOptions: { value: SavingFrequency; label: string }[] = [
  { value: 'NEVER', label: 'Nunca' },
  { value: 'RARELY', label: 'Rara vez' },
  { value: 'MONTHLY', label: 'Mensualmente' },
  { value: 'BIWEEKLY', label: 'Quincenalmente' },
  { value: 'WEEKLY', label: 'Semanalmente' },
  { value: 'DAILY', label: 'A diario' },
]

export const editProfileSchema = z.object({
  name: z.string().min(2, 'Ingresá tu nombre completo'),
  email: z.string().min(1, 'Ingresá tu correo electrónico').pipe(z.email('Ingresá un correo válido')),
  monthlyIncome: z.coerce
    .number({ message: 'Ingresá un ingreso válido' })
    .nonnegative('El ingreso no puede ser negativo'),
  savingFrequency: z.enum(savingFrequencyValues),
  currentPassword: z.string().min(1, 'Ingresá tu contraseña actual para confirmar los cambios'),
})

export type EditProfileFormInput = z.input<typeof editProfileSchema>
export type EditProfileFormValues = z.output<typeof editProfileSchema>

export const changePasswordSchema = z
  .object({
    oldPassword: z.string().min(1, 'Ingresá tu contraseña actual'),
    newPassword: z.string().min(8, 'La nueva contraseña debe tener al menos 8 caracteres'),
    confirmPassword: z.string().min(1, 'Confirmá tu nueva contraseña'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Las contraseñas no coinciden',
    path: ['confirmPassword'],
  })

export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>
