import { zodResolver } from '@hookform/resolvers/zod'
import { useFieldArray, useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { StepHeader } from '../../components/analysis/StepHeader'
import { TransactionRow } from '../../components/analysis/TransactionRow'
import { Button } from '../../components/ui/Button'
import { useAnalysis } from '../../context/useAnalysis'
import {
  transactionsSchema,
  type TransactionsFormInput,
  type TransactionsFormValues,
} from '../../lib/validation/analysisSchemas'

function emptyRow(): TransactionsFormInput['transactions'][number] {
  return {
    desc: '',
    amount: undefined as unknown as number,
    category: '' as never,
    paymentMethod: '' as never,
    bankName: '',
    operationNumber: '',
  }
}

function TransactionsPage() {
  const { transactions, setTransactions } = useAnalysis()
  const navigate = useNavigate()

  const {
    control,
    register,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<TransactionsFormInput, unknown, TransactionsFormValues>({
    resolver: zodResolver(transactionsSchema),
    mode: 'onChange',
    defaultValues: {
      transactions:
        transactions.length > 0
          ? transactions.map((t) => ({
              desc: t.desc,
              amount: t.amount,
              category: t.category,
              paymentMethod: t.paymentMethod,
              bankName: t.bankName ?? '',
              operationNumber: t.operationNumber ?? '',
            }))
          : [
              emptyRow(),
              emptyRow(),
            ],
    },
  })

  const { fields, append, remove } = useFieldArray({ control, name: 'transactions' })

  function onSubmit(values: TransactionsFormValues) {
    setTransactions(values.transactions)
    navigate('/analysis/review')
  }

  return (
    <>
      <StepHeader
        title="Nueva transacción"
        onBack={() => navigate('/dashboard')}
        action={
          <Link
            to="/transactions/upload"
            className="flex items-center gap-1.5 rounded-full border border-border bg-surface px-3.5 py-2 text-[13px] font-semibold text-navy transition hover:border-navy hover:shadow-sm"
          >
            <UploadIcon />
            Subir cartola
          </Link>
        }
      />

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="px-6 lg:px-0">
        <h2 className="mt-6 font-display text-2xl font-bold tracking-tight text-ink">Agregá tus transacciones</h2>
        <p className="mt-2 text-[15px] text-ink-soft">
          Enumerá tus gastos recientes: descripción, monto, categoría y método de pago. Agregá las que quieras.
        </p>

        <div className="mt-6">
          {fields.length === 0 && (
            <p className="py-4 text-center text-sm text-ink-faint">
              Todavía no hay transacciones. Tocá "Agregar transacción" para empezar.
            </p>
          )}
          {fields.map((field, index) => (
            <TransactionRow
              key={field.id}
              descProps={register(`transactions.${index}.desc`)}
              amountProps={register(`transactions.${index}.amount`)}
              control={control}
              index={index}
              paymentMethodProps={register(`transactions.${index}.paymentMethod`)}
              bankNameProps={register(`transactions.${index}.bankName`)}
              operationNumberProps={register(`transactions.${index}.operationNumber`)}
              onRemove={() => remove(index)}
              error={
                errors.transactions?.[index]?.desc?.message ??
                errors.transactions?.[index]?.amount?.message ??
                errors.transactions?.[index]?.category?.message ??
                errors.transactions?.[index]?.paymentMethod?.message
              }
            />
          ))}
          {errors.transactions?.message && (
            <p className="mb-2 text-xs font-medium text-risk">{errors.transactions.message}</p>
          )}
          <button
            type="button"
            onClick={() => append(emptyRow())}
            className="mt-1 flex w-full items-center justify-center gap-1.5 rounded-xl border-[1.5px] border-dashed border-border py-3.5 text-sm font-semibold text-navy transition hover:border-navy"
          >
            + Agregar transacción
          </button>
        </div>

        <div className="mt-8 max-lg:fixed max-lg:inset-x-0 max-lg:bottom-0 max-lg:z-10 max-lg:mt-0 max-lg:border-t max-lg:border-border max-lg:bg-surface max-lg:px-6 max-lg:py-4">
          <Button type="submit" disabled={!isValid}>
            Revisar
          </Button>
        </div>
      </form>
    </>
  )
}

function UploadIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M12 16V4M12 4 7 9M12 4l5 5M4 16v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export default TransactionsPage
