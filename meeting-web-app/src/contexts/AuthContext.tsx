import { createContext, useContext, useState, ReactNode } from 'react'
import { authApi } from '../api/client'

interface AuthState {
  token: string | null
  email: string | null
  role: string | null
}

interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState>(() => ({
    token: localStorage.getItem('token'),
    email: localStorage.getItem('email'),
    role: localStorage.getItem('role'),
  }))

  const login = async (email: string, password: string) => {
    const { data } = await authApi.login(email, password)
    localStorage.setItem('token', data.token)
    localStorage.setItem('email', data.email)
    localStorage.setItem('role', data.role)
    setAuth({ token: data.token, email: data.email, role: data.role })
  }

  const register = async (email: string, password: string) => {
    await authApi.register(email, password)
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('email')
    localStorage.removeItem('role')
    setAuth({ token: null, email: null, role: null })
  }

  return (
    <AuthContext.Provider value={{ ...auth, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
