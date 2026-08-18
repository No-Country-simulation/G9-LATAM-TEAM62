import { api } from './client'

export type SavingFrequency = 'NEVER' | 'RARELY' | 'MONTHLY' | 'BIWEEKLY' | 'WEEKLY' | 'DAILY'
export type FinancialProfile = 'SAVER' | 'BALANCED' | 'SPENDER' | 'AT_RISK'

export interface ApiUser {
  id: number
  name: string
  email: string
  monthlyIncome: number | null
  savingFrequency: SavingFrequency | null
  financialProfile: FinancialProfile | null
  profileAccuracy: number | null
}

export interface AuthResponse {
  token: string
  user: ApiUser
}

const financialProfileLabels: Record<FinancialProfile, string> = {
  SAVER: 'Ahorrador',
  BALANCED: 'Equilibrado',
  SPENDER: 'Gastador',
  AT_RISK: 'En riesgo',
}

export function financialProfileLabel(profile: FinancialProfile | null): string {
  return profile ? financialProfileLabels[profile] : 'Sin calcular todavía'
}

export function loginRequest(email: string, password: string) {
  return api.post<AuthResponse>('/api/auth/login', { email, password })
}

export interface RegisterPayload {
  name: string
  email: string
  password: string
}

export function registerRequest(data: RegisterPayload) {
  return api.post<ApiUser>('/api/auth/register', data)
}
