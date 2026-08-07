import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

/**
 * Waiting on `isLoading` matters: on a fresh page load, AuthContext is still
 * trying to exchange a stored refresh token for a session. Without this
 * check, a logged-in user hitting refresh would flash to /login for a
 * moment before that exchange resolves.
 */
export default function ProtectedRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="font-mono text-xs text-ink-text-dim uppercase tracking-widest">Loading…</p>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return children
}
