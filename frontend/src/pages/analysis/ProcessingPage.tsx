import { useEffect, useRef, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAnalysis } from '../../context/useAnalysis'

const statusMessages = [
  'Revisando tus datos…',
  'Categorizando tus transacciones…',
  'Calculando tu puntaje de salud…',
  'Preparando recomendaciones…',
]

function ProcessingPage() {
  const { profile, transactions, runAnalysis } = useAnalysis()
  const navigate = useNavigate()
  const [statusIndex, setStatusIndex] = useState(0)
  const started = useRef(false)
  const isValid = profile.income != null && transactions.length > 0

  useEffect(() => {
    if (!isValid || started.current) return
    started.current = true

    const interval = setInterval(() => {
      setStatusIndex((i) => (i + 1) % statusMessages.length)
    }, 1100)

    runAnalysis().then(() => {
      clearInterval(interval)
      navigate('/analysis/results', { replace: true })
    })

    return () => clearInterval(interval)
  }, [isValid, runAnalysis, navigate])

  if (!isValid) {
    return <Navigate to="/analysis/new" replace />
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-8 text-center">
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
