import { useEffect, useRef, useState, type ReactNode } from 'react'
import { ApiError } from '../lib/api/client'
import { generateRecommendationsRequest } from '../lib/api/recommendations'
import {
  categoryLabel,
  createManualTransactionRequest,
  type ApiTransaction,
  type TransactionCategory,
} from '../lib/api/transactions'
import { useAuth } from './useAuth'
import { ANALYSIS_STORAGE_KEY, AnalysisContext, type AnalysisResult, type TransactionInput } from './analysis-context'

function readStoredResult(): AnalysisResult | null {
  const stored = sessionStorage.getItem(ANALYSIS_STORAGE_KEY)
  if (!stored) return null
  try {
    const parsed = JSON.parse(stored) as Partial<AnalysisResult>
    if (!Array.isArray(parsed.transactions) || !Array.isArray(parsed.recommendations) || !Array.isArray(parsed.categories)) {
      return null
    }
    return parsed as AnalysisResult
  } catch {
    return null
  }
}

function buildCategories(transactions: ApiTransaction[]) {
  const buckets = new Map<TransactionCategory, number>()
  transactions.forEach((t) => {
    buckets.set(t.category, (buckets.get(t.category) ?? 0) + t.amount)
  })
  return Array.from(buckets.entries()).map(([category, amount]) => ({
    name: categoryLabel(category),
    category,
    amount,
  }))
}

export function AnalysisProvider({ children }: { children: ReactNode }) {
  const { user: authUser } = useAuth()
  const [transactions, setTransactionsState] = useState<TransactionInput[]>([])
  const [lastResult, setLastResult] = useState<AnalysisResult | null>(readStoredResult)
  const [isProcessing, setIsProcessing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const createdSoFarRef = useRef<ApiTransaction[]>([])

  function setTransactions(next: TransactionInput[]) {
    createdSoFarRef.current = []
    setTransactionsState(next)
  }

  useEffect(() => {
    if (lastResult) {
      sessionStorage.setItem(ANALYSIS_STORAGE_KEY, JSON.stringify(lastResult))
    } else {
      sessionStorage.removeItem(ANALYSIS_STORAGE_KEY)
    }
  }, [lastResult])

  async function generateForTransactions(created: ApiTransaction[]) {
    if (!authUser) {
      throw new Error('Necesitás iniciar sesión para analizar tus finanzas.')
    }

    setIsProcessing(true)
    setError(null)
    try {
      const recommendations = await generateRecommendationsRequest(authUser.id)

      const result: AnalysisResult = {
        categories: buildCategories(created),
        recommendations,
        transactions: created,
        date: new Date().toISOString(),
      }
      setLastResult(result)

      return result
    } catch (err) {
      const message =
        err instanceof ApiError ? err.message : 'No pudimos completar el análisis. Intentá nuevamente.'
      setError(message)
      throw err
    } finally {
      setIsProcessing(false)
    }
  }

  async function runAnalysis() {
    if (!authUser) {
      throw new Error('Necesitás iniciar sesión para analizar tus finanzas.')
    }

    setIsProcessing(true)
    setError(null)
    try {
      const createdTransactions = createdSoFarRef.current
      for (let i = createdTransactions.length; i < transactions.length; i++) {
        const t = transactions[i]
        const created = await createManualTransactionRequest({
          userId: authUser.id,
          amount: t.amount,
          category: t.category,
          description: t.desc,
          paymentMethod: t.paymentMethod,
          bankName: t.bankName,
          operationNumber: t.operationNumber,
        })
        createdTransactions.push(created)
        createdSoFarRef.current = createdTransactions
      }

      const result = await generateForTransactions(createdTransactions)
      createdSoFarRef.current = []
      return result
    } catch (err) {
      const message =
        err instanceof ApiError ? err.message : 'No pudimos completar el análisis. Intentá nuevamente.'
      setError(message)
      throw err
    } finally {
      setIsProcessing(false)
    }
  }

  return (
    <AnalysisContext.Provider
      value={{
        transactions,
        setTransactions,
        lastResult,
        isProcessing,
        error,
        runAnalysis,
        generateForTransactions,
      }}
    >
      {children}
    </AnalysisContext.Provider>
  )
}
