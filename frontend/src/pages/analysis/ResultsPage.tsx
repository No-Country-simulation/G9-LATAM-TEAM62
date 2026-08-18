import { Navigate, useNavigate } from 'react-router-dom'
import { CategoryDonut } from '../../components/analysis/CategoryDonut'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { NavyPanel } from '../../components/ui/NavyPanel'
import { ScoreGauge } from '../../components/ui/ScoreGauge'
import { useAnalysis } from '../../context/useAnalysis'
import { useAuth } from '../../context/useAuth'
import { financialProfileLabel } from '../../lib/api/auth'
import { categoryLabel, paymentMethodLabel } from '../../lib/api/transactions'

function ResultsPage() {
  const { lastResult } = useAnalysis()
  const { user } = useAuth()
  const navigate = useNavigate()

  if (!lastResult || !user) {
    return <Navigate to="/dashboard" replace />
  }

  return (
    <div className="min-h-[calc(100dvh-64px)] pb-28 lg:pb-16">
      <div className="mx-auto w-full max-w-[1160px] px-6 lg:px-8">
        <h1 className="pt-6 font-display text-2xl font-bold tracking-tight text-ink lg:pt-10 lg:text-[28px]">
          Tus resultados
        </h1>

        <div className="mt-6 lg:grid lg:grid-cols-[300px_1fr] lg:items-start lg:gap-10">
          <div>
            <NavyPanel className="text-center lg:text-left">
              {user.financialProfile ? (
                <Badge profile={user.financialProfile} />
              ) : (
                <p className="text-[13px] text-white/70">Perfil financiero: sin calcular todavía</p>
              )}
              <div className="mt-4">
                <ScoreGauge
                  score={user.profileAccuracy != null ? user.profileAccuracy * 100 : 0}
                  progressColor={user.profileAccuracy != null ? '#16B892' : 'rgba(255,255,255,.35)'}
                  label="Confianza del perfil"
                  glow={user.profileAccuracy != null}
                />
              </div>
            </NavyPanel>

            <div className="mt-6">
              <div className="text-xs font-bold tracking-wide text-ink-faint uppercase">Desglose de gastos</div>
              <div className="mt-3 rounded-2xl border border-border p-5 text-center">
                <CategoryDonut categories={lastResult.categories} />
              </div>
            </div>
          </div>

          <div className="mt-8 lg:mt-0">
            <div className="flex items-baseline justify-between">
              <div className="text-xs font-bold tracking-wide text-ink-faint uppercase">Recomendaciones</div>
              {lastResult.recommendations.length > 0 && (
                <p className="font-mono text-[11px] text-ink-faint">
                  Generado{' '}
                  {new Date(lastResult.recommendations[0].generatedAt).toLocaleDateString('es-AR', {
                    day: 'numeric',
                    month: 'short',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </p>
              )}
            </div>
            <div className="mt-3">
              {lastResult.recommendations.length === 0 && (
                <div className="rounded-2xl border-[1.5px] border-dashed border-ink-faint/45 px-5 py-8 text-center">
                  <p className="text-sm leading-relaxed text-ink-soft">
                    No encontramos recomendaciones nuevas para este período.
                  </p>
                </div>
              )}
              {lastResult.recommendations.map((rec, i) => (
                <div key={rec.id} className="mt-2.5 flex items-start gap-3 rounded-xl bg-surface-alt p-4 first:mt-0">
                  <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-accent-soft font-mono text-xs font-semibold text-accent-ink">
                    {i + 1}
                  </span>
                  <div>
                    <p className="text-[13.5px] leading-relaxed text-ink">{rec.text}</p>
                    {rec.profileAtGeneration && (
                      <p className="mt-1 text-[11px] text-ink-faint">
                        Según tu perfil: {financialProfileLabel(rec.profileAtGeneration)}
                      </p>
                    )}
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-7 text-xs font-bold tracking-wide text-ink-faint uppercase">
              Transacciones registradas
            </div>
            <div className="mt-3 rounded-2xl border border-border">
              {lastResult.transactions.map((t, i) => (
                <div
                  key={t.id}
                  className={`flex items-center justify-between gap-3 px-5 py-3 ${i > 0 ? 'border-t border-border' : ''}`}
                >
                  <div className="min-w-0">
                    <p className="truncate text-[13.5px] font-medium text-ink">{t.description || 'Sin descripción'}</p>
                    <p className="mt-0.5 text-[11px] text-ink-faint">
                      {categoryLabel(t.category)} · {paymentMethodLabel(t.paymentMethod)} ·{' '}
                      {new Date(t.date).toLocaleDateString('es-AR', { day: 'numeric', month: 'short' })}
                    </p>
                  </div>
                  <span className="shrink-0 font-mono text-sm font-semibold text-ink">
                    ${t.amount.toLocaleString('es-AR')}
                  </span>
                </div>
              ))}
            </div>

            <div className="mt-8 hidden lg:flex lg:justify-end">
              <Button onClick={() => navigate('/dashboard')} fullWidth={false} className="px-10">
                Volver al panel
              </Button>
            </div>
          </div>
        </div>
      </div>

      <div className="fixed inset-x-0 bottom-0 z-10 border-t border-border bg-surface px-6 py-4 lg:hidden">
        <Button onClick={() => navigate('/dashboard')}>Volver al panel</Button>
      </div>
    </div>
  )
}

export default ResultsPage
