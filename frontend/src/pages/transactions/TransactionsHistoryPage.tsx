import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { CategorySelect } from '../../components/ui/CategorySelect'
import { Modal } from '../../components/ui/Modal'
import { useAuth } from '../../context/useAuth'
import { useToast } from '../../context/useToast'
import { ApiError } from '../../lib/api/client'
import {
  deleteTransactionRequest,
  listTransactionsRequest,
  paymentMethodLabel,
  toUpdatePayload,
  updateTransactionCategoryRequest,
  updateTransactionRequest,
  type ApiTransaction,
  type ExpenseCategory,
} from '../../lib/api/transactions'

function TransactionsHistoryPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [transactions, setTransactions] = useState<ApiTransaction[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    listTransactionsRequest(user.id)
      .then((list) => setTransactions([...list].sort((a, b) => b.date.localeCompare(a.date))))
      .catch((err) => setLoadError(err instanceof ApiError ? err.message : 'No se pudieron cargar tus transacciones.'))
  }, [user])

  if (!user) return null

  return (
    <div className="mx-auto w-full max-w-[720px] px-6 pt-6 pb-28 lg:pt-8 lg:pb-16">
      <PageHeader title="Tus transacciones" />

      {loadError && (
        <p className="mt-5 rounded-lg bg-risk/10 px-3 py-2 text-sm font-medium text-risk">{loadError}</p>
      )}

      {transactions == null && !loadError && (
        <p className="mt-8 text-center text-sm text-ink-faint">Cargando…</p>
      )}

      {transactions && transactions.length === 0 && (
        <div className="mt-8 flex flex-col items-center justify-center rounded-2xl border-[1.5px] border-dashed border-ink-faint/45 px-5 py-14 text-center">
          <div className="mx-auto mb-3.5 flex h-11 w-11 items-center justify-center rounded-full bg-accent-soft text-accent-ink">
            <SparkleIcon />
          </div>
          <p className="text-sm leading-relaxed text-ink-soft">
            <strong className="text-ink">Todavía no cargaste transacciones.</strong>
            <br />
            Cargá la primera para empezar.
          </p>
          <Button
            fullWidth={false}
            className="mx-auto mt-5 px-8"
            onClick={() => navigate('/analysis/transactions')}
          >
            Nueva transacción
          </Button>
        </div>
      )}

      {transactions && transactions.length > 0 && (
        <>
          <div className="mt-8 flex items-center gap-8 border-b border-border pb-6">
            <div>
              <p className="text-[11px] font-semibold tracking-wide text-ink-faint uppercase">Transacciones</p>
              <p className="mt-1 font-mono text-2xl font-semibold text-ink">{transactions.length}</p>
            </div>
            <div className="h-10 w-px bg-border" aria-hidden />
            <div>
              <p className="text-[11px] font-semibold tracking-wide text-ink-faint uppercase">Total gastado</p>
              <p className="mt-1 font-mono text-2xl font-semibold text-accent-ink">
                ${transactions.reduce((sum, t) => sum + t.amount, 0).toLocaleString('es-AR')}
              </p>
            </div>
          </div>

          <div className="mt-5 rounded-2xl border border-border">
            {transactions.map((t, i) => (
              <TransactionRow
                key={t.id}
                transaction={t}
                border={i > 0}
                onDeleted={() => setTransactions((prev) => prev?.filter((x) => x.id !== t.id) ?? null)}
                onUpdated={(updated) =>
                  setTransactions((prev) => prev?.map((x) => (x.id === updated.id ? updated : x)) ?? null)
                }
              />
            ))}
          </div>
        </>
      )}
    </div>
  )
}

function TransactionRow({
  transaction,
  border,
  onDeleted,
  onUpdated,
}: {
  transaction: ApiTransaction
  border: boolean
  onDeleted: () => void
  onUpdated: (updated: ApiTransaction) => void
}) {
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isSavingCategory, setIsSavingCategory] = useState(false)
  const [rowError, setRowError] = useState<string | null>(null)
  const [isEditing, setIsEditing] = useState(false)
  const [draft, setDraft] = useState({
    description: transaction.description ?? '',
    amount: String(transaction.amount),
    date: transaction.date,
  })
  const [isSavingEdit, setIsSavingEdit] = useState(false)
  const { showToast } = useToast()

  async function handleDelete() {
    setIsDeleting(true)
    setRowError(null)
    try {
      await deleteTransactionRequest(transaction.id)
      onDeleted()
      showToast('Transacción eliminada')
    } catch (err) {
      setRowError(err instanceof ApiError ? err.message : 'No se pudo eliminar.')
      setIsDeleting(false)
    }
  }

  async function handleCategoryChange(value: ExpenseCategory) {
    if (!value || value === transaction.category) return
    setIsSavingCategory(true)
    setRowError(null)
    try {
      const updated = await updateTransactionCategoryRequest(transaction.id, value)
      onUpdated(updated)
      showToast('Categoría actualizada')
    } catch (err) {
      setRowError(err instanceof ApiError ? err.message : 'No se pudo corregir la categoría.')
    } finally {
      setIsSavingCategory(false)
    }
  }

  function startEditing() {
    setDraft({ description: transaction.description ?? '', amount: String(transaction.amount), date: transaction.date })
    setRowError(null)
    setIsEditing(true)
  }

  async function handleSaveEdit() {
    const amount = Number(draft.amount)
    if (!draft.date || !Number.isFinite(amount) || amount <= 0) {
      setRowError('Revisá el monto y la fecha.')
      return
    }
    setIsSavingEdit(true)
    setRowError(null)
    try {
      const updated = await updateTransactionRequest(
        transaction.id,
        toUpdatePayload(transaction, { description: draft.description || null, amount, date: draft.date }),
      )
      onUpdated(updated)
      setIsEditing(false)
      showToast('Transacción actualizada')
    } catch (err) {
      setRowError(err instanceof ApiError ? err.message : 'No se pudieron guardar los cambios.')
    } finally {
      setIsSavingEdit(false)
    }
  }

  return (
    <div className={`px-5 py-3.5 ${border ? 'border-t border-border' : ''}`}>
      <div className="flex items-center justify-between gap-3">
        <button type="button" onClick={startEditing} className="min-w-0 flex-1 text-left">
          <p className="truncate text-[13.5px] font-medium text-ink hover:underline">
            {transaction.description || 'Sin descripción'}
          </p>
          <p className="mt-0.5 text-[11px] text-ink-faint">
            {paymentMethodLabel(transaction.paymentMethod)} ·{' '}
            {new Date(transaction.date).toLocaleDateString('es-AR', { day: 'numeric', month: 'short', year: 'numeric' })}
          </p>
        </button>
        <span className="shrink-0 font-mono text-sm font-semibold text-ink">
          ${transaction.amount.toLocaleString('es-AR')}
        </span>
      </div>

      <div className="mt-2.5 flex items-center justify-between gap-3">
        <CategorySelect value={transaction.category} onChange={handleCategoryChange} disabled={isSavingCategory} />

        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={startEditing}
            aria-label="Editar transacción"
            className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-ink-faint transition hover:bg-surface-alt hover:text-ink"
          >
            <PencilIcon />
          </button>
          <button
            type="button"
            onClick={() => setConfirmingDelete(true)}
            aria-label="Eliminar transacción"
            className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-ink-faint transition hover:bg-risk-soft hover:text-risk"
          >
            <TrashIcon />
          </button>
        </div>
      </div>

      {rowError && !isEditing && !confirmingDelete && (
        <p className="mt-1.5 text-xs font-medium text-risk">{rowError}</p>
      )}

      <Modal isOpen={isEditing} onClose={() => setIsEditing(false)} title="Editar transacción">
        {rowError && <p className="mb-3 text-xs font-medium text-risk">{rowError}</p>}
        <div className="flex flex-col gap-3">
          <input
            type="text"
            value={draft.description}
            onChange={(e) => setDraft((d) => ({ ...d, description: e.target.value }))}
            placeholder="Descripción"
            className="w-full rounded-lg border-[1.5px] border-border bg-transparent px-3 py-2 text-sm text-ink outline-none focus:border-navy"
          />
          <input
            type="number"
            inputMode="decimal"
            value={draft.amount}
            onChange={(e) => setDraft((d) => ({ ...d, amount: e.target.value }))}
            className="w-full rounded-lg border-[1.5px] border-border bg-transparent px-3 py-2 text-right font-mono text-sm text-ink outline-none focus:border-navy"
          />
          <input
            type="date"
            value={draft.date}
            onChange={(e) => setDraft((d) => ({ ...d, date: e.target.value }))}
            className="w-full rounded-lg border-[1.5px] border-border bg-transparent px-3 py-2 text-sm text-ink outline-none focus:border-navy"
          />
        </div>
        <div className="mt-5 flex justify-end gap-3">
          <Button variant="ghost" fullWidth={false} onClick={() => setIsEditing(false)} disabled={isSavingEdit}>
            Cancelar
          </Button>
          <Button fullWidth={false} isLoading={isSavingEdit} onClick={handleSaveEdit}>
            Guardar
          </Button>
        </div>
      </Modal>

      <Modal isOpen={confirmingDelete} onClose={() => setConfirmingDelete(false)} title="Eliminar transacción">
        {rowError && <p className="mb-3 text-xs font-medium text-risk">{rowError}</p>}
        <p className="text-sm text-ink-soft">¿Confirmás que querés eliminar esta transacción? No se puede deshacer.</p>
        <div className="mt-5 flex justify-end gap-3">
          <Button variant="ghost" fullWidth={false} onClick={() => setConfirmingDelete(false)} disabled={isDeleting}>
            Cancelar
          </Button>
          <Button
            fullWidth={false}
            isLoading={isDeleting}
            onClick={handleDelete}
            className="bg-risk text-white hover:shadow-[0_10px_24px_-8px_rgba(225,72,77,.5)]"
          >
            Eliminar
          </Button>
        </div>
      </Modal>
    </div>
  )
}

function PencilIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path
        d="M12 20h9M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function TrashIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path
        d="M4 7h16M9 7V4.5A1.5 1.5 0 0 1 10.5 3h3A1.5 1.5 0 0 1 15 4.5V7m2 0v13a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V7h10Z"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function SparkleIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path
        d="M12 3v4M12 17v4M3 12h4M17 12h4M5.6 5.6l2.8 2.8M15.6 15.6l2.8 2.8M18.4 5.6l-2.8 2.8M8.4 15.6l-2.8 2.8"
        strokeLinecap="round"
      />
    </svg>
  )
}

export default TransactionsHistoryPage
