import { createContext } from 'react'
import type { ApiRecommendation } from '../lib/api/recommendations'
import type { ApiTransaction, ExpenseCategory, PaymentMethod, TransactionCategory } from '../lib/api/transactions'

export const ANALYSIS_STORAGE_KEY = 'financeai.lastAnalysis'

export interface CategoryBreakdown {
  name: string
  category: TransactionCategory
  amount: number
}

export interface AnalysisResult {
  categories: CategoryBreakdown[]
  recommendations: ApiRecommendation[]
  transactions: ApiTransaction[]
  date: string
}

export interface TransactionInput {
  desc: string
  amount: number
  category: ExpenseCategory
  paymentMethod: PaymentMethod
  bankName?: string
  operationNumber?: string
}

export interface AnalysisContextValue {
  transactions: TransactionInput[]
  setTransactions: (transactions: TransactionInput[]) => void
  lastResult: AnalysisResult | null
  isProcessing: boolean
  error: string | null
  runAnalysis: () => Promise<AnalysisResult>
  generateForTransactions: (created: ApiTransaction[]) => Promise<AnalysisResult>
}

export const AnalysisContext = createContext<AnalysisContextValue | null>(null)
