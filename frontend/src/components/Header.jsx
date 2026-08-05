import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Header() {
  const { user, isAuthenticated, logout } = useAuth()

  return (
    <header className="border-b border-ink-text/10 px-6 py-4 flex items-center justify-between">
      <Link to="/" className="font-display text-sm font-semibold tracking-widest uppercase">
        UPI Payment Gateway Simulator
      </Link>

      {isAuthenticated ? (
        <div className="flex items-center gap-4">
          <span className="font-mono text-xs text-ink-text-dim hidden sm:inline">
            {user?.fullName}
          </span>
          <button
            type="button"
            onClick={logout}
            className="font-mono text-xs text-debit underline underline-offset-4"
          >
            Log out
          </button>
        </div>
      ) : (
        <div className="flex items-center gap-4 font-mono text-xs">
          <Link to="/login" className="text-ink-text-dim hover:text-ink-text">
            Sign in
          </Link>
          <Link to="/register" className="text-credit underline underline-offset-4">
            Open an account
          </Link>
        </div>
      )}
    </header>
  )
}
