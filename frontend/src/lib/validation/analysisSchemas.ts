import { z } from 'zod'
import { debtLevels, savingsFrequencies } from '../../context/analysis-context'

export const profileSchema = z.object({
  income: z.coerce
    .number({ message: 'Ingresá un ingreso válido' })
    .positive('El ingreso debe ser mayor a 0'),
  debt: z.string().refine((value) => (debtLevels as readonly string[]).includes(value), {
    message: 'Elegí tu nivel de deuda actual',
  }),
  savings: z.string().refine((value) => (savingsFrequencies as readonly string[]).includes(value), {
    message: 'Elegí una frecuencia de ahorro',
  }),
})

export type ProfileFormInput = z.input<typeof profileSchema>
export type ProfileFormValues = z.output<typeof profileSchema>

export const transactionRowSchema = z.object({
  desc: z.string().min(1, 'Agregá una descripción'),
  amount: z.coerce.number({ message: 'Ingresá un monto' }).positive('El monto debe ser mayor a 0'),
})

export const transactionsSchema = z.object({
  transactions: z.array(transactionRowSchema).min(1, 'Agregá al menos una transacción'),
})

export type TransactionsFormInput = z.input<typeof transactionsSchema>
export type TransactionsFormValues = z.output<typeof transactionsSchema>
