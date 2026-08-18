import { useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { NavyPanel } from '../components/ui/NavyPanel'
import { Reveal } from '../components/ui/Reveal'
import { ScoreGauge } from '../components/ui/ScoreGauge'
import { useAuth } from '../context/useAuth'
import { ApiError } from '../lib/api/client'
import { getRecommendationHistoryRequest, type ApiRecommendation } from '../lib/api/recommendations'
import { categoryLabel, listTransactionsRequest, type ApiTransaction } from '../lib/api/transactions'

const RECENT_TRANSACTIONS_LIMIT = 5
const RECENT_RECOMMENDATIONS_LIMIT = 3

function firstName(fullName: string) {
  return fullName.trim().split(/\s+/)[0] ?? fullName
}

function DashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [recentTransactions, setRecentTransactions] = useState<ApiTransaction[] | null>(null)
  const [recentError, setRecentError] = useState<string | null>(null)
  const [recentRecommendations, setRecentRecommendations] = useState<ApiRecommendation[] | null>(null)
  const [recommendationsError, setRecommendationsError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    listTransactionsRequest(user.id)
      .then((list) =>
        setRecentTransactions([...list].sort((a, b) => b.date.localeCompare(a.date)).slice(0, RECENT_TRANSACTIONS_LIMIT)),
      )
      .catch((err) => setRecentError(err instanceof ApiError ? err.message : 'No se pudieron cargar tus últimos movimientos.'))

    getRecommendationHistoryRequest(user.id)
      .then((list) =>
        setRecentRecommendations(
          [...list].sort((a, b) => b.generatedAt.localeCompare(a.generatedAt)).slice(0, RECENT_RECOMMENDATIONS_LIMIT),
        ),
      )
      .catch((err) =>
        setRecommendationsError(err instanceof ApiError ? err.message : 'No se pudieron cargar tus recomendaciones.'),
      )
  }, [user])

  if (!user) {
    return <Navigate to="/login" replace />
  }

  return (
    <div className="pb-28 lg:flex lg:h-[calc(100dvh-64px)] lg:flex-col lg:overflow-y-auto lg:pb-0">
      <div className="mx-auto flex w-full max-w-[1240px] flex-1 flex-col px-6 pt-6 lg:px-10 lg:pt-8">
        <div className="flex items-end justify-between">
          <div>
            <p className="font-display text-[13px] font-semibold tracking-wide text-ink-faint uppercase">
              Qué bueno verte de nuevo
            </p>
            <h2 className="mt-1 font-display text-[26px] font-bold tracking-tight text-ink lg:text-3xl">
              {firstName(user.name)}
            </h2>
          </div>
          <Button
            onClick={() => navigate('/analysis/transactions')}
            fullWidth={false}
            className="hidden px-8 lg:flex"
          >
            Nueva transacción
          </Button>
        </div>

        <div className="mt-6 lg:grid lg:min-h-[460px] lg:grid-cols-[300px_1fr_300px] lg:items-stretch lg:gap-7">
          <Reveal className="lg:h-full">
            <NavyPanel className="text-center lg:flex lg:h-full lg:flex-col lg:justify-center">
              <ScoreGauge
                score={user.profileAccuracy != null ? user.profileAccuracy * 100 : 0}
                size={136}
                stroke={10}
                progressColor={user.profileAccuracy != null ? '#16B892' : 'rgba(255,255,255,.35)'}
                label="Confianza del perfil"
                glow={user.profileAccuracy != null}
                animate
              />
              {user.financialProfile ? (
                <div className="mt-4 flex flex-col items-center gap-2.5">
                  <Badge profile={user.financialProfile} />
                </div>
              ) : (
                <p className="mt-4 text-[13px] leading-relaxed text-white/70">
                  Tu perfil financiero todavía no fue calculado. Lo genera el motor de IA del equipo de
                  datos cuando esté disponible.
                </p>
              )}
            </NavyPanel>
          </Reveal>

          <Reveal delayMs={120} className="mt-8 lg:mt-0 lg:flex lg:h-full lg:flex-col">
            <div className="flex items-center justify-between">
              <div className="text-xs font-bold tracking-wide text-ink-faint uppercase">Últimas transacciones</div>
              {recentTransactions && recentTransactions.length > 0 && (
                <button
                  type="button"
                  onClick={() => navigate('/transactions')}
                  className="text-[13px] font-semibold text-navy hover:underline"
                >
                  Ver todas
                </button>
              )}
            </div>

            <div className="mt-3 lg:flex-1">
              {recentError && (
                <p className="rounded-lg bg-risk/10 px-3 py-2 text-sm font-medium text-risk">{recentError}</p>
              )}

              {recentTransactions == null && !recentError && (
                <p className="py-6 text-center text-sm text-ink-faint">Cargando…</p>
              )}

              {recentTransactions && recentTransactions.length === 0 && (
                <div className="flex flex-col items-center justify-center rounded-2xl border-[1.5px] border-dashed border-ink-faint/45 px-5 py-10 text-center lg:h-full">
                  <div className="mx-auto mb-3.5 flex h-11 w-11 items-center justify-center rounded-full bg-accent-soft text-accent-ink">
                    <SparkleIcon />
                  </div>
                  <p className="text-sm leading-relaxed text-ink-soft">
                    <strong className="text-ink">Todavía no hay movimientos.</strong>
                    <br />
                    Cargá tus transacciones para ver tu desglose de gastos y recomendaciones personalizadas.
                  </p>
                </div>
              )}

              {recentTransactions && recentTransactions.length > 0 && (
                <div className="rounded-2xl border border-border lg:flex lg:h-full lg:flex-col">
                  {recentTransactions.map((t, i) => (
                    <div
                      key={t.id}
                      className={`flex items-center justify-between gap-3 px-4 py-3 lg:flex-1 ${i > 0 ? 'border-t border-border' : ''}`}
                    >
                      <div className="min-w-0">
                        <p className="truncate text-[13px] font-medium text-ink">
                          {t.description || 'Sin descripción'}
                        </p>
                        <p className="mt-0.5 truncate text-[11px] text-ink-faint">
                          {categoryLabel(t.category)} ·{' '}
                          {new Date(t.date).toLocaleDateString('es-AR', { day: 'numeric', month: 'short' })}
                        </p>
                      </div>
                      <span className="shrink-0 font-mono text-[13px] font-semibold text-ink">
                        ${t.amount.toLocaleString('es-AR')}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </Reveal>

          <Reveal delayMs={280} className="mt-8 lg:mt-0 lg:flex lg:h-full lg:flex-col">
            <div className="flex items-center justify-between">
              <div className="text-xs font-bold tracking-wide text-ink-faint uppercase">Recomendaciones recientes</div>
              {recentRecommendations && recentRecommendations.length > 0 && (
                <button
                  type="button"
                  onClick={() => navigate('/recommendations')}
                  className="text-[13px] font-semibold text-navy hover:underline"
                >
                  Ver todas
                </button>
              )}
            </div>

            <div className="mt-3 lg:flex-1">
              {recommendationsError && (
                <p className="rounded-lg bg-risk/10 px-3 py-2 text-sm font-medium text-risk">{recommendationsError}</p>
              )}

              {recentRecommendations == null && !recommendationsError && (
                <p className="py-6 text-center text-sm text-ink-faint">Cargando…</p>
              )}

              {recentRecommendations && recentRecommendations.length === 0 && (
                <div className="flex flex-col items-center justify-center rounded-2xl border-[1.5px] border-dashed border-ink-faint/45 px-5 py-10 text-center lg:h-full">
                  <div className="mx-auto mb-3.5 flex h-11 w-11 items-center justify-center rounded-full bg-accent-soft text-accent-ink">
                    <SparkleIcon />
                  </div>
                  <p className="text-sm leading-relaxed text-ink-soft">
                    <strong className="text-ink">Todavía no tenés recomendaciones generadas.</strong>
                    <br />
                    Necesitás al menos 5 transacciones cargadas para generar las primeras.
                  </p>
                </div>
              )}

              {recentRecommendations &&
                recentRecommendations.map((rec, i) => (
                  <div
                    key={rec.id}
                    className={`flex items-start gap-3 rounded-xl bg-surface-alt p-4 ${i > 0 ? 'mt-2.5' : ''}`}
                  >
                    <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-accent-soft font-mono text-xs font-semibold text-accent-ink">
                      {i + 1}
                    </span>
                    <div className="min-w-0">
                      <p className="text-[13px] leading-relaxed text-ink">{rec.text}</p>
                      <p className="mt-1 font-mono text-[11px] text-ink-faint">
                        {new Date(rec.generatedAt).toLocaleDateString('es-AR', { day: 'numeric', month: 'short' })}
                      </p>
                    </div>
                  </div>
                ))}
            </div>
          </Reveal>
        </div>
      </div>

      <div className="fixed inset-x-0 bottom-0 z-10 border-t border-border bg-surface px-6 py-4 lg:hidden">
        <Button onClick={() => navigate('/analysis/transactions')}>Nueva transacción</Button>
      </div>
    </div>
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

export default DashboardPage
