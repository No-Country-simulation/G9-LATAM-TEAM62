import { useEffect, useState, type ReactNode } from 'react'
import {
  AnalysisContext,
  type AnalysisResult,
  type ProfileInput,
  type TransactionInput,
} from './analysis-context'

const STORAGE_KEY = 'financeai.lastAnalysis'
const PROCESSING_DELAY_MS = 2600

const DEBT_PENALTY: Record<string, number> = { Ninguna: 0, Baja: 5, Moderada: 12, Alta: 22 }
const SAVINGS_BONUS: Record<string, number> = { Nunca: 0, Ocasionalmente: 6, Mensualmente: 12, Semanalmente: 16 }

const CATEGORY_RULES: [RegExp, string][] = [
  [/alquiler|hipoteca|vivienda|expensas/i, 'Vivienda'],
  [/super|almacen|restaurante|comida|delivery|cafe/i, 'Comida y restaurantes'],
  [/uber|nafta|combustible|transporte|colectivo|subte/i, 'Transporte'],
  [/netflix|spotify|suscrip|gimnasio/i, 'Suscripciones'],
]

const emptyProfile: ProfileInput = { income: null, debt: null, savings: null }

function wait(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function readStoredResult(): AnalysisResult | null {
  const stored = sessionStorage.getItem(STORAGE_KEY)
  return stored ? (JSON.parse(stored) as AnalysisResult) : null
}

function computeAnalysis(profile: ProfileInput, transactions: TransactionInput[]): AnalysisResult {
  const income = profile.income ?? 0
  const totalSpend = transactions.reduce((sum, t) => sum + t.amount, 0)
  const ratio = income > 0 ? totalSpend / income : 1
  const debtPenalty = profile.debt ? DEBT_PENALTY[profile.debt] : 10
  const savingsBonus = profile.savings ? SAVINGS_BONUS[profile.savings] : 0

  let score = Math.round(100 - ratio * 100 * 0.6 - debtPenalty + savingsBonus)
  score = Math.max(5, Math.min(97, score))
  const financialProfile = score >= 70 ? 'Saludable' : score >= 40 ? 'Necesita atención' : 'En riesgo'

  const buckets: Record<string, number> = {
    Vivienda: 0,
    'Comida y restaurantes': 0,
    Transporte: 0,
    Suscripciones: 0,
    Otros: 0,
  }
  transactions.forEach((t) => {
    const match = CATEGORY_RULES.find(([re]) => re.test(t.desc))
    buckets[match ? match[1] : 'Otros'] += t.amount
  })
  const categories = Object.entries(buckets)
    .filter(([, amount]) => amount > 0)
    .map(([name, amount]) => ({ name, amount }))

  const recommendations = [
    ratio > 0.7
      ? 'Tu gasto está muy cerca de tu ingreso total: buscá un gasto recurrente para recortar este mes.'
      : 'Tu gasto deja un margen saludable frente a tu ingreso: mantené esa diferencia estable.',
    profile.savings === 'Nunca' || profile.savings === 'Ocasionalmente'
      ? 'Programá una transferencia automática, aunque sea pequeña, justo después de cobrar.'
      : 'Mantené tu hábito de ahorro regular: la constancia importa más que el monto.',
    profile.debt === 'Alta' || profile.debt === 'Moderada'
      ? 'Priorizá pagar la deuda con mayor interés antes de sumar nuevos gastos.'
      : 'Tenés poca presión de deuda: buen momento para hacer crecer un fondo de emergencia.',
  ]

  return { score, profile: financialProfile, categories, recommendations, date: new Date().toISOString() }
}

export function AnalysisProvider({ children }: { children: ReactNode }) {
  const [profile, setProfile] = useState<ProfileInput>(emptyProfile)
  const [transactions, setTransactions] = useState<TransactionInput[]>([])
  const [lastResult, setLastResult] = useState<AnalysisResult | null>(readStoredResult)
  const [isProcessing, setIsProcessing] = useState(false)

  useEffect(() => {
    if (lastResult) {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(lastResult))
    } else {
      sessionStorage.removeItem(STORAGE_KEY)
    }
  }, [lastResult])

  async function runAnalysis() {
    setIsProcessing(true)
    await wait(PROCESSING_DELAY_MS)
    const result = computeAnalysis(profile, transactions)
    setLastResult(result)
    setIsProcessing(false)
    return result
  }

  return (
    <AnalysisContext.Provider
      value={{
        profile,
        setProfile,
        transactions,
        setTransactions,
        lastResult,
        isProcessing,
        runAnalysis,
      }}
    >
      {children}
    </AnalysisContext.Provider>
  )
}
