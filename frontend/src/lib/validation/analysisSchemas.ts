import { z } from 'zod'
import { expenseCategoryValues, paymentMethodValues } from '../api/transactions'

export const transactionRowSchema = z.object({
  desc: z
    .string()
    .min(10, 'La descripción debe tener al menos 10 caracteres')
    .max(200, 'La descripción no puede superar los 200 caracteres'),
  amount: z.coerce.number({ message: 'Ingresá un monto' }).positive('El monto debe ser mayor a 0'),
  category: z.enum(expenseCategoryValues, { message: 'Elegí una categoría' }),
  paymentMethod: z.enum(paymentMethodValues, { message: 'Elegí un método de pago' }),
  bankName: z.preprocess(
    (v) => (v === '' || v === undefined ? undefined : v),
    z.string().max(100, 'Máximo 100 caracteres').optional(),
  ),
  operationNumber: z.preprocess(
    (v) => (v === '' || v === undefined ? undefined : v),
    z
      .string()
      .max(50, 'Máximo 50 caracteres')
      .regex(/^[a-zA-Z0-9-]*$/, 'Solo letras, números y guiones')
      .optional(),
  ),
})

export const transactionsSchema = z.object({
  transactions: z.array(transactionRowSchema).min(1, 'Agregá al menos una transacción'),
})

export type TransactionsFormInput = z.input<typeof transactionsSchema>
export type TransactionsFormValues = z.output<typeof transactionsSchema>
