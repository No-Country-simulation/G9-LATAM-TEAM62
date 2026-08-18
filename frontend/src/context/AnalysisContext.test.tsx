import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ApiUser } from '../lib/api/auth'
import { SESSION_KEY, setToken } from '../lib/api/client'
import type { ApiTransaction } from '../lib/api/transactions'
import { AuthProvider } from './AuthContext'
import { AnalysisProvider } from './AnalysisContext'
import { useAnalysis } from './useAnalysis'
import { useAuth } from './useAuth'

vi.mock('../lib/api/transactions', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../lib/api/transactions')>()
  return {
    ...actual,
    createManualTransactionRequest: vi.fn(),
  }
})
vi.mock('../lib/api/recommendations', () => ({
  generateRecommendationsRequest: vi.fn(),
}))

import { createManualTransactionRequest } from '../lib/api/transactions'
import { generateRecommendationsRequest } from '../lib/api/recommendations'

const createManualMock = vi.mocked(createManualTransactionRequest)
const generateRecommendationsMock = vi.mocked(generateRecommendationsRequest)

const baseUser: ApiUser = {
  id: 1,
  name: 'Ana',
  email: 'ana@test.com',
  monthlyIncome: 500000,
  savingFrequency: 'MONTHLY',
  financialProfile: null,
  profileAccuracy: null,
}

function makeTransaction(overrides: Partial<ApiTransaction>): ApiTransaction {
  return {
    id: 1,
    description: null,
    operationNumber: null,
    amount: 100,
    category: 'FOOD',
    date: new Date().toISOString(),
    currency: { id: 1, name_currency: 'CLP' },
    balanceAfter: null,
    userId: 1,
    source: 'MANUAL',
    paymentMethod: 'CASH',
    linkStatus: 'UNLINKED',
    linkedTransactionId: null,
    categoryMethod: null,
    categoryConfidence: null,
    bankName: null,
    type: 'EXPENSE',
    ...overrides,
  }
}

function wrapper({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <AnalysisProvider>{children}</AnalysisProvider>
    </AuthProvider>
  )
}

function useHarness() {
  return { analysis: useAnalysis(), auth: useAuth() }
}

function loginAs(user: ApiUser) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(user))
  setToken('token-123')
}

describe('AnalysisContext', () => {
  beforeEach(() => {
    sessionStorage.clear()
    createManualMock.mockReset()
    generateRecommendationsMock.mockReset()
  })

  it('un reintento tras una falla parcial no vuelve a crear las transacciones ya persistidas', async () => {
    loginAs(baseUser)
    const { result } = renderHook(useHarness, { wrapper })

    act(() => {
      result.current.analysis.setTransactions([
        { desc: 'Supermercado', amount: 100, category: 'FOOD', paymentMethod: 'CASH' },
        { desc: 'Nafta', amount: 50, category: 'TRANSPORT', paymentMethod: 'CASH' },
      ])
    })

    const createdFirst = makeTransaction({ id: 10, description: 'Supermercado', amount: 100 })
    createManualMock
      .mockResolvedValueOnce(createdFirst)
      .mockRejectedValueOnce(new Error('network down'))

    await act(async () => {
      await expect(result.current.analysis.runAnalysis()).rejects.toThrow()
    })

    expect(createManualMock).toHaveBeenCalledTimes(2)

    const createdSecond = makeTransaction({ id: 11, description: 'Nafta', amount: 50, category: 'TRANSPORT' })
    createManualMock.mockResolvedValueOnce(createdSecond)
    generateRecommendationsMock.mockResolvedValueOnce([
      { id: 1, text: 'Vas bien', generatedAt: new Date().toISOString(), profileAtGeneration: null, userId: 1 },
    ])

    await act(async () => {
      await result.current.analysis.runAnalysis()
    })

    expect(createManualMock).toHaveBeenCalledTimes(3)
    const thirdCallPayload = createManualMock.mock.calls[2][0]
    expect(thirdCallPayload.description).toBe('Nafta')

    expect(result.current.analysis.lastResult?.transactions).toHaveLength(2)
  })

  it('generateForTransactions arma el resultado a partir de las transacciones y recomendaciones', async () => {
    loginAs(baseUser)
    const { result } = renderHook(useHarness, { wrapper })

    const created = [
      makeTransaction({ id: 20, category: 'FOOD', amount: 200 }),
      makeTransaction({ id: 21, category: 'FOOD', amount: 300 }),
    ]
    generateRecommendationsMock.mockResolvedValueOnce([
      { id: 5, text: 'Reducí el gasto en comida', generatedAt: new Date().toISOString(), profileAtGeneration: 'AT_RISK', userId: 1 },
    ])

    await act(async () => {
      await result.current.analysis.generateForTransactions(created)
    })

    expect(result.current.analysis.lastResult?.categories).toEqual([{ name: 'Comida', category: 'FOOD', amount: 500 }])
    expect(result.current.analysis.lastResult?.recommendations).toHaveLength(1)

    expect(result.current.auth.user?.financialProfile).toBe(baseUser.financialProfile)
    expect(result.current.auth.user?.profileAccuracy).toBe(baseUser.profileAccuracy)
  })
})
