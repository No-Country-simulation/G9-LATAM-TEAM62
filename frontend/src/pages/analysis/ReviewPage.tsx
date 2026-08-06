import { Navigate, useNavigate } from 'react-router-dom'
import { StepHeader } from '../../components/analysis/StepHeader'
import { Button } from '../../components/ui/Button'
import { useAnalysis } from '../../context/useAnalysis'

function ReviewPage() {
  const { profile, transactions } = useAnalysis()
  const navigate = useNavigate()

  if (profile.income == null || profile.debt == null || profile.savings == null || transactions.length === 0) {
    return <Navigate to="/analysis/new" replace />
  }

  return (
    <>
      <StepHeader title="Revisar y enviar" onBack={() => navigate('/analysis/transactions')} />

      <div className="px-6 lg:px-0">
        <p className="mt-4 text-[15px] text-ink-soft">
          Verificá que todo esté correcto antes de ejecutar el análisis.
        </p>

        <div className="mt-6">
          <h3 className="mb-2.5 text-xs font-bold tracking-wide text-ink-faint uppercase">Perfil financiero</h3>
          <div className="rounded-2xl border border-border p-5">
            <SummaryRow label="Ingreso mensual" value={`$${profile.income.toLocaleString('es-AR')}`} />
            <SummaryRow label="Nivel de deuda" value={profile.debt} border />
            <SummaryRow label="Frecuencia de ahorro" value={profile.savings} border />
          </div>
        </div>

        <div className="mt-6">
          <h3 className="mb-2.5 text-xs font-bold tracking-wide text-ink-faint uppercase">Transacciones</h3>
          <div className="rounded-2xl border border-border p-5">
            {transactions.map((t, i) => (
              <SummaryRow
                key={`${t.desc}-${i}`}
                label={t.desc}
                value={`$${t.amount.toLocaleString('es-AR')}`}
                border={i > 0}
              />
            ))}
          </div>
        </div>

        <div className="mt-8 max-lg:fixed max-lg:inset-x-0 max-lg:bottom-0 max-lg:z-10 max-lg:mt-0 max-lg:border-t max-lg:border-border max-lg:bg-surface max-lg:px-6 max-lg:py-4">
          <Button variant="accent" onClick={() => navigate('/analysis/processing')}>
            ✦ Analizar con IA
          </Button>
        </div>
      </div>
    </>
  )
}

function SummaryRow({ label, value, border = false }: { label: string; value: string; border?: boolean }) {
  return (
    <div className={`flex items-center justify-between py-2.5 ${border ? 'border-t border-border' : ''}`}>
      <span className="text-[13px] text-ink-soft">{label}</span>
      <span className="font-mono text-sm font-semibold text-ink">{value}</span>
    </div>
  )
}

export default ReviewPage
