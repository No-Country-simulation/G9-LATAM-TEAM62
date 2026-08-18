import { api } from './client'

export type TransactionCategory =
  | 'FOOD'
  | 'TRANSPORT'
  | 'HOUSING'
  | 'UTILITIES'
  | 'ENTERTAINMENT'
  | 'HEALTH'
  | 'EDUCATION'
  | 'SHOPPING'
  | 'SALARY'
  | 'INVESTMENT'
  | 'SAVINGS'
  | 'OTHER_INCOME'
  | 'OTHER_EXPENSE'

export type PaymentMethod = 'CASH' | 'DEBIT' | 'CREDIT'
export type TransactionSource = 'BANK' | 'MANUAL'
export type LinkStatus = 'LINKED' | 'UNLINKED'
export type TransactionType = 'INCOME' | 'EXPENSE' | 'SAVING'

export const expenseCategoryValues = [
  'FOOD',
  'TRANSPORT',
  'HOUSING',
  'UTILITIES',
  'ENTERTAINMENT',
  'HEALTH',
  'EDUCATION',
  'SHOPPING',
  'OTHER_EXPENSE',
] as const

export type ExpenseCategory = (typeof expenseCategoryValues)[number]

export const expenseCategoryOptions: { value: ExpenseCategory; label: string }[] = [
  { value: 'FOOD', label: 'Comida' },
  { value: 'TRANSPORT', label: 'Transporte' },
  { value: 'HOUSING', label: 'Vivienda' },
  { value: 'UTILITIES', label: 'Servicios' },
  { value: 'ENTERTAINMENT', label: 'Entretenimiento' },
  { value: 'HEALTH', label: 'Salud' },
  { value: 'EDUCATION', label: 'Educación' },
  { value: 'SHOPPING', label: 'Compras' },
  { value: 'OTHER_EXPENSE', label: 'Otro gasto' },
]

export function categoryLabel(category: TransactionCategory): string {
  return expenseCategoryOptions.find((o) => o.value === category)?.label ?? category
}

const categoryColorMap: Partial<Record<TransactionCategory, string>> = {
  FOOD: '#2a78d6',
  TRANSPORT: '#eb6834',
  HOUSING: '#1baf7a',
  UTILITIES: '#eda100',
  ENTERTAINMENT: '#e87ba4',
  HEALTH: '#008300',
  EDUCATION: '#4a3aa7',
  SHOPPING: '#e34948',
}
const OTHER_CATEGORY_COLOR = '#98a2b3'

export function categoryColor(category: TransactionCategory): string {
  return categoryColorMap[category] ?? OTHER_CATEGORY_COLOR
}

export const paymentMethodValues = ['CASH', 'DEBIT', 'CREDIT'] as const

export const paymentMethodOptions: { value: PaymentMethod; label: string }[] = [
  { value: 'CASH', label: 'Efectivo' },
  { value: 'DEBIT', label: 'Débito' },
  { value: 'CREDIT', label: 'Crédito' },
]

export function paymentMethodLabel(method: PaymentMethod | null): string {
  if (!method) return 'Sin definir'
  return paymentMethodOptions.find((o) => o.value === method)?.label ?? method
}

export interface ApiTransaction {
  id: number
  description: string | null
  operationNumber: string | null
  amount: number
  category: TransactionCategory
  date: string
  currency: { id: number; name_currency: string }
  balanceAfter: number | null
  userId: number
  source: TransactionSource
  paymentMethod: PaymentMethod | null
  linkStatus: LinkStatus
  linkedTransactionId: number | null
  categoryMethod: string | null
  categoryConfidence: number | null
  bankName: string | null
  type: TransactionType
}

export function listTransactionsRequest(userId: number) {
  return api.get<ApiTransaction[]>(`/api/transactions?userId=${userId}`)
}

export interface StatementIngestionResult {
  status: string
  fileName: string
  country: string
  year: number
  rawRowsCount: number
  validRowsCount: number
  discardedRowsCount: number
  warnings: string[]
  createdTransactions: ApiTransaction[]
}

export function uploadStatementRequest(
  file: File,
  userId: number,
  options?: { defaultYear?: number; country?: string },
) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('userId', String(userId))
  if (options?.defaultYear) formData.append('defaultYear', String(options.defaultYear))
  if (options?.country) formData.append('country', options.country)
  return api.postForm<StatementIngestionResult>('/api/transactions/upload-statement', formData)
}

export interface ManualTransactionPayload {
  userId: number
  amount: number
  category: TransactionCategory
  description?: string
  paymentMethod: PaymentMethod
  bankName?: string
  operationNumber?: string
}

export function createManualTransactionRequest(payload: ManualTransactionPayload) {
  return api.post<ApiTransaction>('/api/transactions/manual', {
    currency: { name_currency: 'CLP' },
    ...payload,
  })
}

export function updateTransactionCategoryRequest(id: number, category: TransactionCategory) {
  return api.put<ApiTransaction>(`/api/transactions/${id}/category`, { category })
}

export interface UpdateTransactionPayload {
  description: string | null
  operationNumber: string | null
  amount: number
  category: TransactionCategory
  date: string
  currency: { id: number }
  balanceAfter: number | null
  userId: number
  source: TransactionSource
  paymentMethod: PaymentMethod | null
  linkStatus: LinkStatus
  linkedTransactionId: number | null
  categoryMethod: string | null
  categoryConfidence: number | null
  bankName: string | null
}

export function updateTransactionRequest(id: number, payload: UpdateTransactionPayload) {
  return api.put<ApiTransaction>(`/api/transactions/${id}`, payload)
}

export function toUpdatePayload(
  t: ApiTransaction,
  overrides: Partial<Pick<UpdateTransactionPayload, 'description' | 'amount' | 'date' | 'category'>>,
): UpdateTransactionPayload {
  return {
    description: overrides.description ?? t.description,
    operationNumber: t.operationNumber,
    amount: overrides.amount ?? t.amount,
    category: overrides.category ?? t.category,
    date: overrides.date ?? t.date,
    currency: { id: t.currency.id },
    balanceAfter: t.balanceAfter,
    userId: t.userId,
    source: t.source,
    paymentMethod: t.paymentMethod,
    linkStatus: t.linkStatus,
    linkedTransactionId: t.linkedTransactionId,
    categoryMethod: t.categoryMethod,
    categoryConfidence: t.categoryConfidence,
    bankName: t.bankName,
  }
}

export function deleteTransactionRequest(id: number) {
  return api.delete<void>(`/api/transactions/${id}`)
}
