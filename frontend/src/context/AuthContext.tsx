import { useEffect, useState, type ReactNode } from 'react'
import { loginRequest, registerRequest } from '../lib/api/auth'
import { SESSION_KEY, setToken } from '../lib/api/client'
import { ANALYSIS_STORAGE_KEY } from './analysis-context'
import { AuthContext, type AuthUser, type LoginInput, type RegisterInput } from './auth-context'

function readStoredUser(): AuthUser | null {
  const stored = sessionStorage.getItem(SESSION_KEY)
  return stored ? (JSON.parse(stored) as AuthUser) : null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(readStoredUser)

  useEffect(() => {
    if (user) {
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(user))
    } else {
      sessionStorage.removeItem(SESSION_KEY)
    }
  }, [user])

  async function login({ email, password }: LoginInput) {
    const { token, user: apiUser } = await loginRequest(email, password)
    setToken(token)
    setUser(apiUser)
  }

  async function register({ name, email, password }: RegisterInput) {
    await registerRequest({ name, email, password })
    await login({ email, password })
  }

  function logout() {
    setToken(null)
    sessionStorage.removeItem(SESSION_KEY)
    sessionStorage.removeItem(ANALYSIS_STORAGE_KEY)
    window.location.href = '/'
  }

  function updateUser(updated: AuthUser) {
    setUser(updated)
  }

  return (
    <AuthContext.Provider value={{ user, login, register, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  )
}
