import { useEffect, useState, type ReactNode } from 'react'
import { AuthContext, type AuthUser, type LoginInput, type RegisterInput } from './auth-context'

const STORAGE_KEY = 'financeai.session'
const MOCK_DELAY_MS = 600

function deriveNameFromEmail(email: string) {
  const local = email.split('@')[0] || 'Usuario'
  return local.charAt(0).toUpperCase() + local.slice(1)
}

function wait(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function readStoredUser(): AuthUser | null {
  const stored = sessionStorage.getItem(STORAGE_KEY)
  return stored ? (JSON.parse(stored) as AuthUser) : null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(readStoredUser)

  useEffect(() => {
    if (user) {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(user))
    } else {
      sessionStorage.removeItem(STORAGE_KEY)
    }
  }, [user])

  async function login({ email }: LoginInput) {
    await wait(MOCK_DELAY_MS)
    setUser({ name: deriveNameFromEmail(email), email })
  }

  async function register({ name, email }: RegisterInput) {
    await wait(MOCK_DELAY_MS)
    setUser({ name, email })
  }

  function logout() {
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}
