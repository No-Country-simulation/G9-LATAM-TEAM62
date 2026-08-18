import { z } from 'zod'

export const loginSchema = z.object({
  email: z.string().min(1, 'Ingresá tu correo electrónico').pipe(z.email('Ingresá un correo válido')),
  password: z.string().min(8, 'La contraseña debe tener al menos 8 caracteres'),
})

export type LoginFormValues = z.infer<typeof loginSchema>

export const registerSchema = z
  .object({
    name: z.string().min(2, 'Ingresá tu nombre completo'),
    email: z.string().min(1, 'Ingresá tu correo electrónico').pipe(z.email('Ingresá un correo válido')),
    password: z
      .string()
      .min(8, 'La contraseña debe tener al menos 8 caracteres')
      .regex(/(?=.*[A-Z])(?=.*\d)/, 'Debe incluir al menos una mayúscula y un número'),
    confirmPassword: z.string().min(1, 'Confirmá tu contraseña'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Las contraseñas no coinciden',
    path: ['confirmPassword'],
  })

export type RegisterFormValues = z.infer<typeof registerSchema>
