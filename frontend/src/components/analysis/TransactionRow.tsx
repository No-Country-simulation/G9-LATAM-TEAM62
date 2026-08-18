import { Controller, useWatch, type Control, type UseFormRegisterReturn } from 'react-hook-form'
import { paymentMethodOptions } from '../../lib/api/transactions'
import type { TransactionsFormInput } from '../../lib/validation/analysisSchemas'
import { CategorySelect } from '../ui/CategorySelect'

interface TransactionRowProps {
  descProps: UseFormRegisterReturn
  amountProps: UseFormRegisterReturn
  control: Control<TransactionsFormInput>
  index: number
  paymentMethodProps: UseFormRegisterReturn
  bankNameProps: UseFormRegisterReturn
  operationNumberProps: UseFormRegisterReturn
  onRemove: () => void
  error?: string
}

export function TransactionRow({
  descProps,
  amountProps,
  control,
  index,
  paymentMethodProps,
  bankNameProps,
  operationNumberProps,
  onRemove,
  error,
}: TransactionRowProps) {
  const paymentMethod = useWatch({ control, name: `transactions.${index}.paymentMethod` })
  const showBankFields = paymentMethod === 'DEBIT' || paymentMethod === 'CREDIT'

  return (
    <div className="mb-2.5">
      <div className="flex items-center gap-2 rounded-xl bg-surface-alt p-2.5">
        <input
          {...descProps}
          type="text"
          placeholder="Descripción (ej. Supermercado)"
          className="min-w-0 flex-1 rounded-lg bg-transparent px-2 py-1.5 text-sm text-ink outline-none focus:bg-white"
        />
        <input
          {...amountProps}
          type="number"
          inputMode="decimal"
          placeholder="0.00"
          className="w-24 shrink-0 rounded-lg bg-transparent px-2 py-1.5 text-right font-mono text-sm font-semibold text-ink outline-none focus:bg-white"
        />
        <button
          type="button"
          onClick={onRemove}
          aria-label="Quitar transacción"
          className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-lg leading-none text-ink-faint transition hover:bg-risk-soft hover:text-risk"
        >
          ×
        </button>
      </div>
      <div className="mt-1.5 flex gap-1.5">
        <Controller
          control={control}
          name={`transactions.${index}.category`}
          render={({ field }) => (
            <CategorySelect className="min-w-0 flex-1" value={field.value ?? ''} onChange={field.onChange} onBlur={field.onBlur} />
          )}
        />
        <select
          {...paymentMethodProps}
          className="min-w-0 flex-1 rounded-lg border-[1.5px] border-border bg-transparent px-2.5 py-1.5 text-xs font-medium text-ink-soft outline-none focus:border-navy"
        >
          <option value="">Método de pago</option>
          {paymentMethodOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>
      {showBankFields && (
        <div className="mt-1.5 flex gap-1.5">
          <input
            {...bankNameProps}
            type="text"
            placeholder="Banco (opcional)"
            className="min-w-0 flex-1 rounded-lg border-[1.5px] border-border bg-transparent px-2.5 py-1.5 text-xs text-ink outline-none focus:border-navy"
          />
          <input
            {...operationNumberProps}
            type="text"
            placeholder="Nº de operación (opcional)"
            className="min-w-0 flex-1 rounded-lg border-[1.5px] border-border bg-transparent px-2.5 py-1.5 text-xs text-ink outline-none focus:border-navy"
          />
        </div>
      )}
      {error && <p className="mt-1 pl-1 text-xs font-medium text-risk">{error}</p>}
    </div>
  )
}
