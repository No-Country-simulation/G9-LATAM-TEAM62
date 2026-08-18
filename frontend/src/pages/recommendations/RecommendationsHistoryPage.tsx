import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { useAuth } from '../../context/useAuth'
import { useToast } from '../../context/useToast'
import { financialProfileLabel } from '../../lib/api/auth'
import { ApiError } from '../../lib/api/client'
import {
  generateRecommendationsRequest,
  getRecommendationHistoryRequest,
  type ApiRecommendation,
} from '../../lib/api/recommendations'

function RecommendationsHistoryPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [recommendations, setRecommendations] = useState<ApiRecommendation[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isGenerating, setIsGenerating] = useState(false)

  useEffect(() => {
    if (!user) return
    getRecommendationHistoryRequest(user.id)
      .then((list) => setRecommendations([...list].sort((a, b) => b.generatedAt.localeCompare(a.generatedAt))))
      .catch((err) => setLoadError(err instanceof ApiError ? err.message : 'No se pudieron cargar tus recomendaciones.'))
  }, [user])

  async function handleGenerate() {
    if (!user) return
    setIsGenerating(true)
    try {
      const generated = await generateRecommendationsRequest(user.id)
      if (generated.length > 0) {
        setRecommendations((prev) => [...generated, ...(prev ?? [])])
        showToast(`Se generaron ${generated.length} recomendación${generated.length > 1 ? 'es' : ''} nueva${generated.length > 1 ? 's' : ''}`)
      } else {
        showToast('No hay recomendaciones nuevas: puede ser por el cooldown de 7 días, pocas transacciones cargadas, o que tu gasto está dentro de lo esperado.')
      }
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : 'No se pudieron generar recomendaciones.', 'error')
    } finally {
      setIsGenerating(false)
    }
  }

  if (!user) return null

  return (
    <div className="mx-auto w-full max-w-[720px] px-6 pt-6 pb-28 lg:pt-8 lg:pb-16">
      <div className="flex items-center justify-between gap-3">
        <PageHeader title="Tus recomendaciones" />
        <Button fullWidth={false} onClick={handleGenerate} isLoading={isGenerating} className="shrink-0 px-6">
          Generar recomendaciones
        </Button>
      </div>

      {loadError && (
        <p className="mt-5 rounded-lg bg-risk/10 px-3 py-2 text-sm font-medium text-risk">{loadError}</p>
      )}

      {recommendations == null && !loadError && (
        <p className="mt-8 text-center text-sm text-ink-faint">Cargando…</p>
      )}

      {recommendations && recommendations.length === 0 && (
        <div className="mt-8 flex flex-col items-center justify-center rounded-2xl border-[1.5px] border-dashed border-ink-faint/45 px-5 py-14 text-center">
          <div className="mx-auto mb-3.5 flex h-11 w-11 items-center justify-center rounded-full bg-accent-soft text-accent-ink">
            <SparkleIcon />
          </div>
          <p className="text-sm leading-relaxed text-ink-soft">
            <strong className="text-ink">Todavía no tenés recomendaciones.</strong>
            <br />
            Necesitás al menos 5 transacciones cargadas para generar las primeras.
          </p>
          <Button fullWidth={false} className="mt-5 px-8" onClick={() => navigate('/analysis/transactions')}>
            Nueva transacción
          </Button>
        </div>
      )}

      {recommendations && recommendations.length > 0 && (
        <div className="mt-8">
          {recommendations.map((rec, i) => (
            <div
              key={rec.id}
              className={`flex items-start gap-3 rounded-xl bg-surface-alt p-4 ${i > 0 ? 'mt-2.5' : ''}`}
            >
              <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-accent-soft font-mono text-xs font-semibold text-accent-ink">
                {i + 1}
              </span>
              <div className="min-w-0">
                <p className="text-[13.5px] leading-relaxed text-ink">{rec.text}</p>
                <p className="mt-1 font-mono text-[11px] text-ink-faint">
                  {new Date(rec.generatedAt).toLocaleDateString('es-AR', {
                    day: 'numeric',
                    month: 'short',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                  {rec.profileAtGeneration && ` · Perfil: ${financialProfileLabel(rec.profileAtGeneration)}`}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
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

export default RecommendationsHistoryPage
