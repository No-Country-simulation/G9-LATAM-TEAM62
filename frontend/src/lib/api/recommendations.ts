import { api } from './client'
import type { FinancialProfile } from './auth'

export interface ApiRecommendation {
  id: number
  text: string
  generatedAt: string
  profileAtGeneration: FinancialProfile | null
  userId: number
}

export function generateRecommendationsRequest(userId: number, periodStart?: string, periodEnd?: string) {
  const params = new URLSearchParams({ userId: String(userId) })
  if (periodStart) params.set('periodStart', periodStart)
  if (periodEnd) params.set('periodEnd', periodEnd)
  return api.post<ApiRecommendation[]>(`/api/recommendations/generate?${params.toString()}`)
}

export function getRecommendationHistoryRequest(userId: number) {
  return api.get<ApiRecommendation[]>(`/api/recommendations/user/${userId}`)
}
