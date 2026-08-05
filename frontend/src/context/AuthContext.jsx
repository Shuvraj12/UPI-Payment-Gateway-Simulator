import { createContext, useContext, useCallback, useEffect, useState } from 'react'
import * as authService from '../services/authService.js'
import { setAccessToken as syncApiAccessToken } from '../services/api.js'

const AuthContext = createContext(null)

const REFRESH_TOKEN_KEY = 'upi_simulator_refresh_token'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [accessToken, setAccessToken] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    syncApiAccessToken(accessToken)
  }, [accessToken])

  const applySession = useCallback((data) => {
    setUser(data.user)
    setAccessToken(data.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
  }, [])

  const clearSession = useCallback(() => {
    setUser(null)
    setAccessToken(null)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }, [])

  // On first load, try to turn a stored refresh token back into a live
  // session - this is what makes "log in, then refresh the browser" keep
  // you logged in instead of dropping you back to the anonymous homepage.
  useEffect(() => {
    const storedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    if (!storedRefreshToken) {
      setIsLoading(false)
      return
    }
    authService
      .refresh(storedRefreshToken)
      .then(applySession)
      .catch(() => localStorage.removeItem(REFRESH_TOKEN_KEY))
      .finally(() => setIsLoading(false))
    // Deliberately runs once on mount only.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const register = useCallback(async (payload) => {
    const data = await authService.register(payload)
    applySession(data)
    return data
  }, [applySession])

  const login = useCallback(async (payload) => {
    const data = await authService.login(payload)
    applySession(data)
    return data
  }, [applySession])

  const logout = useCallback(async () => {
    const storedRefreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
    clearSession()
    if (storedRefreshToken) {
      // Best-effort: the local session is already cleared either way, so a
      // network failure here shouldn't block the user from appearing logged out.
      await authService.logout(storedRefreshToken).catch(() => {})
    }
  }, [clearSession])

  const value = {
    user,
    accessToken,
    isAuthenticated: Boolean(accessToken),
    isLoading,
    register,
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
