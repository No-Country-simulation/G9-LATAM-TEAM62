import { zodResolver } from '@hookform/resolvers/zod'
import { useFieldArray, useForm } from 'react-hook-form'
import { Navigate, useNavigate } from 'react-router-dom'
import { StepHeader } from '../../components/analysis/StepHeader'
import { TransactionRow } from '../../components/analysis/TransactionRow'
import { Button } from '../../components/ui/Button'
import { useAnalysis } from '../../context/useAnalysis'
import {
  transactionsSchema,
  type TransactionsFormInput,
  type TransactionsFormValues,
} from '../../lib/validation/analysisSchemas'

function TransactionsPage() {
  const { profile, transactions, setTransactions } = useAnalysis()
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
          ? transactions.map((t) => ({ desc: t.desc, amount: t.amount }))
          : [
              { desc: '', amount: undefined as unknown as number },
              { desc: '', amount: undefined as unknown as number },
            ],
    },
  })

  const { fields, append, remove } = useFieldArray({ control, name: 'transactions' })

  if (profile.income == null) {
    return <Navigate to="/analysis/new" replace />
  }

  function onSubmit(values: TransactionsFormValues) {
    setTransactions(values.transactions)
    navigate('/analysis/review')
  }

  return (
    <>
      <StepHeader title="Nuevo análisis" onBack={() => navigate('/analysis/new')} step={2} totalSteps={2} />

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="px-6 lg:px-0">
        <h2 className="mt-6 font-display text-2xl font-bold tracking-tight text-ink">Agregá tus transacciones</h2>
        <p className="mt-2 text-[15px] text-ink-soft">
          Enumerá tus gastos recientes: descripción y monto. Agregá las que quieras.
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
              onRemove={() => remove(index)}
              error={errors.transactions?.[index]?.desc?.message ?? errors.transactions?.[index]?.amount?.message}
            />
          ))}
          {errors.transactions?.message && (
            <p className="mb-2 text-xs font-medium text-risk">{errors.transactions.message}</p>
          )}
          <button
            type="button"
            onClick={() => append({ desc: '', amount: undefined as unknown as number })}
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

export default TransactionsPage
