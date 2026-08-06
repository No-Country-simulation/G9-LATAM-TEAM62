import { Navigate, useNavigate } from 'react-router-dom'
import { CategoryDonut } from '../../components/analysis/CategoryDonut'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { ScoreGauge } from '../../components/ui/ScoreGauge'
import { useAnalysis } from '../../context/useAnalysis'

function gaugeColor(score: number) {
  if (score >= 70) return '#16B892'
  if (score >= 40) return '#E2963B'
  return '#E1484D'
}

function ResultsPage() {
  const { lastResult } = useAnalysis()
  const navigate = useNavigate()

  if (!lastResult) {
    return <Navigate to="/dashboard" replace />
  }

  return (
    <div className="min-h-screen pb-28 lg:pb-16">
      <div className="mx-auto w-full max-w-[1160px] px-6 lg:px-8">
        <h1 className="pt-6 font-display text-[17px] font-semibold lg:pt-10">Tus resultados</h1>

        <div className="mt-5 lg:grid lg:grid-cols-[300px_1fr] lg:items-start lg:gap-8">
          <div className="text-center lg:sticky lg:top-8 lg:text-left">
            <Badge profile={lastResult.profile} />
            <div className="mt-4">
              <ScoreGauge
                score={lastResult.score}
                trackColor="#EFF2F6"
                progressColor={gaugeColor(lastResult.score)}
                scoreClassName="text-[#101828]"
                labelClassName="text-[#66707F]"
              />
            </div>
          </div>

          <div className="mt-8 lg:mt-0">
            <div className="text-xs font-bold tracking-wide text-ink-faint uppercase">Desglose de gastos</div>
            <div className="mt-3 rounded-2xl border border-border p-5 text-center">
              <CategoryDonut categories={lastResult.categories} />
            </div>

            <div className="mt-7 text-xs font-bold tracking-wide text-ink-faint uppercase">Recomendaciones</div>
            <div className="mt-3">
              {lastResult.recommendations.map((rec, i) => (
                <div key={rec} className="mt-2.5 flex items-start gap-3 rounded-xl bg-surface-alt p-4 first:mt-0">
                  <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-accent-soft font-mono text-xs font-semibold text-accent-ink">
                    {i + 1}
                  </span>
                  <p className="text-[13.5px] leading-relaxed text-ink">{rec}</p>
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
