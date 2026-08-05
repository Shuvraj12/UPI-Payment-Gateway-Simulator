import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import Header from '../components/Header.jsx'

const inputClass =
  'w-full rounded-md bg-ink-soft border border-ink-text/15 px-3 py-2 font-body text-sm text-ink-text ' +
  'placeholder:text-ink-text-dim/60 focus:outline-none focus:border-credit'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', phoneNumber: '', password: '' })
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  function handleChange(event) {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await register(form)
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.message ?? 'Could not open the account. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-1 px-6 py-14 flex items-start justify-center">
        <div className="w-full max-w-sm">
          <p className="font-mono text-xs uppercase tracking-widest text-stamp">New entry</p>
          <h1 className="font-display text-2xl font-semibold mt-2">Open your ledger</h1>
          <p className="mt-2 text-sm text-ink-text-dim">
            One account, one running ledger. No wallet balance yet — that starts in Phase 4.
          </p>

          <form onSubmit={handleSubmit} className="mt-8 space-y-4">
            <div>
              <label htmlFor="fullName" className="block font-mono text-xs text-ink-text-dim mb-1">
                Full name
              </label>
              <input
                id="fullName"
                name="fullName"
                type="text"
                required
                autoComplete="name"
                value={form.fullName}
                onChange={handleChange}
                className={inputClass}
              />
            </div>

            <div>
              <label htmlFor="email" className="block font-mono text-xs text-ink-text-dim mb-1">
                Email
              </label>
              <input
                id="email"
                name="email"
                type="email"
                required
                autoComplete="email"
                value={form.email}
                onChange={handleChange}
                className={inputClass}
              />
            </div>

            <div>
              <label htmlFor="phoneNumber" className="block font-mono text-xs text-ink-text-dim mb-1">
                Phone number
              </label>
              <input
                id="phoneNumber"
                name="phoneNumber"
                type="tel"
                required
                autoComplete="tel"
                placeholder="10-digit mobile number"
                value={form.phoneNumber}
                onChange={handleChange}
                className={inputClass}
              />
            </div>

            <div>
              <label htmlFor="password" className="block font-mono text-xs text-ink-text-dim mb-1">
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                required
                minLength={8}
                autoComplete="new-password"
                value={form.password}
                onChange={handleChange}
                className={inputClass}
              />
            </div>

            {error && <p className="font-mono text-xs text-debit">{error}</p>}

            <button
              type="submit"
              disabled={submitting}
              className="w-full rounded-md bg-credit text-ink font-display text-sm font-semibold py-2.5 disabled:opacity-60"
            >
              {submitting ? 'Opening…' : 'Open account'}
            </button>
          </form>

          <p className="mt-6 text-center font-mono text-xs text-ink-text-dim">
            Already have a ledger?{' '}
            <Link to="/login" className="text-credit underline underline-offset-4">
              Sign in
            </Link>
          </p>
        </div>
      </main>
    </div>
  )
}
