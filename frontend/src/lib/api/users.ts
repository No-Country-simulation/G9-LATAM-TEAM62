import { api } from './client'
import type { ApiUser, FinancialProfile, SavingFrequency } from './auth'

export function getUserRequest(id: number) {
  return api.get<ApiUser>(`/api/users/${id}`)
}

export interface ProfileHistoryEntry {
  id: number
  userId: number
  financialProfile: FinancialProfile
  profileAccuracy: number
  createdAt: string
}

export function getProfileHistoryRequest(id: number) {
  return api.get<ProfileHistoryEntry[]>(`/api/users/${id}/profile-history`)
}

export interface UpdateUserPayload {
  name: string
  email: string
  password: string
  monthlyIncome: number | null
  savingFrequency: SavingFrequency | null
}

export function updateUserRequest(id: number, payload: UpdateUserPayload) {
  return api.put<ApiUser>(`/api/users/${id}`, payload)
}

export function changePasswordRequest(oldPassword: string, newPassword: string) {
  return api.post<{ message: string }>('/api/users/change-password', { oldPassword, newPassword })
}

export function deleteUserRequest(id: number) {
  return api.delete<void>(`/api/users/${id}`)
}
