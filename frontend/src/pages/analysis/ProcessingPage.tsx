import { useEffect, useRef, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { useAnalysis } from '../../context/useAnalysis'

const statusMessages = [
  'Revisando tus datos…',
  'Categorizando tus transacciones…',
  'Guardando tus movimientos…',
  'Preparando recomendaciones…',
]

function ProcessingPage() {
  const { transactions, runAnalysis } = useAnalysis()
  const navigate = useNavigate()
  const [statusIndex, setStatusIndex] = useState(0)
  const [failed, setFailed] = useState<string | null>(null)
  const started = useRef(false)
  const isValid = transactions.length > 0

  useEffect(() => {
    if (!isValid || started.current) return
    started.current = true

    const interval = setInterval(() => {
      setStatusIndex((i) => (i + 1) % statusMessages.length)
    }, 1100)

    runAnalysis()
      .then(() => {
        clearInterval(interval)
        navigate('/analysis/results', { replace: true })
      })
      .catch((err) => {
        clearInterval(interval)
        setFailed(err instanceof Error ? err.message : 'No pudimos completar el análisis. Intentá nuevamente.')
      })

    return () => clearInterval(interval)
  }, [isValid, runAnalysis, navigate])

  if (!isValid) {
    return <Navigate to="/analysis/transactions" replace />
  }

  if (failed) {
    return (
      <div className="flex min-h-[calc(100dvh-64px)] flex-col items-center justify-center px-8 text-center">
        <div className="mb-6 flex h-[72px] w-[72px] items-center justify-center rounded-full bg-risk-soft text-2xl text-risk">
          !
        </div>
        <h2 className="font-display text-xl font-bold text-ink">No pudimos analizar tus finanzas</h2>
        <p className="mt-2.5 max-w-[36ch] text-sm text-ink-soft">{failed}</p>
        <div className="mt-6 flex gap-3">
          <Button
            variant="ghost"
            fullWidth={false}
            onClick={() => {
              started.current = false
              setFailed(null)
            }}
          >
            Reintentar
          </Button>
          <Button fullWidth={false} onClick={() => navigate('/analysis/review')}>
            Volver a revisar
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-8 text-center lg:min-h-[calc(100dvh-64px)]">
      <div className="relative mb-8 flex h-[120px] w-[120px] items-center justify-center rounded-full border-[3px] border-accent-soft">
        <div className="absolute -inset-[3px] animate-spin rounded-full border-[3px] border-transparent border-t-accent border-r-accent" />
        <div className="h-11 w-11 animate-pulse rounded-full bg-navy" />
      </div>
      <h2 className="font-display text-xl font-bold text-ink">Analizando tus finanzas</h2>
      <p className="mt-2.5 min-h-5 text-sm text-ink-soft">{statusMessages[statusIndex]}</p>
    </div>
  )
}

export default ProcessingPage
