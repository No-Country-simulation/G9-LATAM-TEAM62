import { Navigate, useNavigate } from 'react-router-dom'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { ScoreGauge } from '../components/ui/ScoreGauge'
import { useAnalysis } from '../context/useAnalysis'
import { useAuth } from '../context/useAuth'

function gaugeColor(score: number) {
  if (score >= 70) return '#16B892'
  if (score >= 40) return '#E2963B'
  return '#E1484D'
}

function DashboardPage() {
  const { user, logout } = useAuth()
  const { lastResult } = useAnalysis()
  const navigate = useNavigate()

  if (!user) {
    return <Navigate to="/login" replace />
  }

  function handleLogout() {
    logout()
    navigate('/')
  }

  return (
    <div className="min-h-screen pb-28 lg:pb-16">
      <div className="mx-auto w-full max-w-[1160px] px-6 lg:px-8">
        <div className="flex items-start justify-between pt-6 lg:pt-10">
          <div>
            <p className="text-sm font-medium text-ink-soft">Qué bueno verte de nuevo</p>
            <h2 className="mt-0.5 font-display text-2xl font-bold">{user.name}</h2>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="mt-1 text-sm font-semibold text-ink-soft transition hover:text-ink lg:hidden"
          >
            Cerrar sesión
          </button>
        </div>

        <div className="mt-5 lg:grid lg:grid-cols-[320px_1fr] lg:items-start lg:gap-6">
          <div className="rounded-[26px] bg-navy px-5 pt-7 pb-6 text-center text-white shadow-[0_20px_44px_-14px_rgba(16,27,51,.22)] lg:sticky lg:top-8">
            <ScoreGauge
              score={lastResult?.score ?? 0}
              trackColor="rgba(255,255,255,.14)"
              progressColor={lastResult ? gaugeColor(lastResult.score) : 'rgba(255,255,255,.35)'}
              scoreClassName="text-white"
              labelClassName="text-white/65"
            />
            {lastResult ? (
              <div className="mt-3.5">
                <Badge profile={lastResult.profile} />
              </div>
            ) : (
              <p className="mt-3.5 text-[13px] leading-relaxed text-white/70">
                Todavía no tenés un análisis. Hacé tu primer chequeo para ver tu puntaje.
              </p>
            )}
          </div>

          <div className="mt-8 lg:mt-0">
            <div className="text-xs font-bold tracking-wide text-ink-faint uppercase">Último análisis</div>
            <div className="mt-3">
              {lastResult ? (
                <div className="rounded-2xl border border-border p-5">
                  <SummaryRow
                    label="Fecha"
                    value={new Date(lastResult.date).toLocaleDateString('es-AR', {
                      month: 'short',
                      day: 'numeric',
                      year: 'numeric',
                    })}
                  />
                  <SummaryRow label="Perfil" value={lastResult.profile} border />
                  <SummaryRow label="Puntaje de salud" value={`${Math.round(lastResult.score)} / 100`} border />
                </div>
              ) : (
                <div className="rounded-2xl border-[1.5px] border-dashed border-border px-5 py-7 text-center">
                  <div className="mx-auto mb-3.5 flex h-11 w-11 items-center justify-center rounded-full bg-surface-alt text-lg">
                    📊
                  </div>
                  <p className="text-sm leading-relaxed text-ink-soft">
                    <strong className="text-ink">Todavía no hay análisis.</strong>
                    <br />
                    Hacé tu primer chequeo para ver tu puntaje de salud financiera y recomendaciones personalizadas.
                  </p>
                </div>
              )}
            </div>

            <div className="mt-8 hidden lg:flex lg:justify-end">
              <Button onClick={() => navigate('/analysis/new')} fullWidth={false} className="px-10">
                Nuevo análisis
              </Button>
            </div>
          </div>
        </div>
      </div>

      <div className="fixed inset-x-0 bottom-0 z-10 border-t border-border bg-surface px-6 py-4 lg:hidden">
        <Button onClick={() => navigate('/analysis/new')}>Nuevo análisis</Button>
      </div>
    </div>
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

export default DashboardPage
