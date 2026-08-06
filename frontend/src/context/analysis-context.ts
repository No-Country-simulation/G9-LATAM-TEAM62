import { createContext } from 'react'

export const debtLevels = ['Ninguna', 'Baja', 'Moderada', 'Alta'] as const
export const savingsFrequencies = ['Nunca', 'Ocasionalmente', 'Mensualmente', 'Semanalmente'] as const

export type DebtLevel = (typeof debtLevels)[number]
export type SavingsFrequency = (typeof savingsFrequencies)[number]
export type FinancialProfile = 'Saludable' | 'Necesita atención' | 'En riesgo'

export interface ProfileInput {
  income: number | null
  debt: DebtLevel | null
  savings: SavingsFrequency | null
}

export interface CategoryBreakdown {
  name: string
  amount: number
}

export interface AnalysisResult {
  score: number
  profile: FinancialProfile
  categories: CategoryBreakdown[]
  recommendations: string[]
  date: string
}

export interface TransactionInput {
  desc: string
  amount: number
}

export interface AnalysisContextValue {
  profile: ProfileInput
  setProfile: (profile: ProfileInput) => void
  transactions: TransactionInput[]
  setTransactions: (transactions: TransactionInput[]) => void
  lastResult: AnalysisResult | null
  isProcessing: boolean
  runAnalysis: () => Promise<AnalysisResult>
}

export const AnalysisContext = createContext<AnalysisContextValue | null>(null)
