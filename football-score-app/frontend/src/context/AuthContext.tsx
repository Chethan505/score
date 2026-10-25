import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { authApi } from '../services/api'
import type { User, AuthResponse } from '../types'

interface AuthContextType {
  user: User | null
  loading: boolean
  login: (phoneNumber: string) => Promise<void>
  verifyOtp: (phoneNumber: string, otp: string) => Promise<void>
  logout: () => void
  refreshTokens: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const initAuth = async () => {
      const accessToken = localStorage.getItem('accessToken')
      const refreshToken = localStorage.getItem('refreshToken')
      
      if (accessToken && refreshToken) {
        try {
          const response = await authApi.refreshToken(refreshToken)
          localStorage.setItem('accessToken', response.accessToken)
          localStorage.setItem('refreshToken', response.refreshToken)
          setUser({ id: response.userId, phoneNumber: '', whatsappOptIn: false, timezone: 'Asia/Kolkata' })
        } catch {
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
        }
      }
      setLoading(false)
    }
    
    initAuth()
  }, [])

  const login = async (phoneNumber: string) => {
    await authApi.sendOtp(phoneNumber)
  }

  const verifyOtp = async (phoneNumber: string, otp: string) => {
    const response = await authApi.verifyOtp(phoneNumber, otp)
    localStorage.setItem('accessToken', response.accessToken)
    localStorage.setItem('refreshToken', response.refreshToken)
    setUser({ id: response.userId, phoneNumber, whatsappOptIn: false, timezone: 'Asia/Kolkata' })
  }

  const logout = () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    setUser(null)
  }

  const refreshTokens = async () => {
    const refreshToken = localStorage.getItem('refreshToken')
    if (refreshToken) {
      const response = await authApi.refreshToken(refreshToken)
      localStorage.setItem('accessToken', response.accessToken)
      localStorage.setItem('refreshToken', response.refreshToken)
    }
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, verifyOtp, logout, refreshTokens }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}