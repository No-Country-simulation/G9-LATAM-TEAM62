import type { UseFormRegisterReturn } from 'react-hook-form'

interface TransactionRowProps {
  descProps: UseFormRegisterReturn
  amountProps: UseFormRegisterReturn
  onRemove: () => void
  error?: string
}

export function TransactionRow({ descProps, amountProps, onRemove, error }: TransactionRowProps) {
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
      {error && <p className="mt-1 pl-1 text-xs font-medium text-risk">{error}</p>}
    </div>
  )
}
