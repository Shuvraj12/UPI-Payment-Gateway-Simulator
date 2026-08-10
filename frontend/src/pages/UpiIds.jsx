import { useEffect, useRef, useState } from 'react'
import Header from '../components/Header.jsx'
import * as upiIdService from '../services/upiIdService.js'

const inputClass =
  'w-full rounded-md bg-ink-soft border border-ink-text/15 px-3 py-2 font-body text-sm text-ink-text ' +
  'placeholder:text-ink-text-dim/60 focus:outline-none focus:border-credit'

export default function UpiIds() {
  const [upiIds, setUpiIds] = useState([])
  const [loadError, setLoadError] = useState(null)

  const [username, setUsername] = useState('')
  const [availability, setAvailability] = useState(null)
  const [checking, setChecking] = useState(false)
  const latestQuery = useRef('')

  const [createStatus, setCreateStatus] = useState({ error: null, submitting: false })
  const [actionError, setActionError] = useState(null)
  const [busy, setBusy] = useState(false)

  async function loadUpiIds() {
    try {
      const data = await upiIdService.getUpiIds()
      setUpiIds(data)
    } catch {
      setLoadError('Could not load your UPI IDs. Try refreshing the page.')
    }
  }

  useEffect(() => {
    loadUpiIds()
  }, [])

  // Debounced, stale-response-safe live availability check as the user types.
  useEffect(() => {
    const trimmed = username.trim()
    if (!trimmed) {
      setAvailability(null)
      setChecking(false)
      return undefined
    }

    setChecking(true)
    latestQuery.current = trimmed

    const timer = setTimeout(() => {
      upiIdService
        .checkAvailability(trimmed)
        .then((result) => {
          if (latestQuery.current === trimmed) {
            setAvailability(result)
            setChecking(false)
          }
        })
        .catch(() => {
          if (latestQuery.current === trimmed) {
            setChecking(false)
          }
        })
    }, 500)

    return () => clearTimeout(timer)
  }, [username])

  async function handleCreate(event) {
    event.preventDefault()
    setCreateStatus({ error: null, submitting: true })
    try {
      await upiIdService.createUpiId(username.trim())
      setUsername('')
      setAvailability(null)
      setCreateStatus({ error: null, submitting: false })
      loadUpiIds()
    } catch (err) {
      setCreateStatus({
        error: err.response?.data?.message ?? 'Could not create that UPI ID.',
        submitting: false,
      })
    }
  }

  async function handleSetDefault(upiIdId) {
    setBusy(true)
    setActionError(null)
    try {
      await upiIdService.setDefaultUpiId(upiIdId)
      loadUpiIds()
    } catch (err) {
      setActionError(err.response?.data?.message ?? 'Could not update the default UPI ID.')
    } finally {
      setBusy(false)
    }
  }

  if (loadError) {
    return (
      <div className="min-h-screen flex flex-col">
        <Header />
        <div className="flex-1 flex items-center justify-center px-6">
          <p className="font-mono text-sm text-debit">{loadError}</p>
        </div>
      </div>
    )
  }

  const canSubmit = username.trim().length > 0 && availability?.available && !checking

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-1 px-6 py-14">
        <div className="mx-auto max-w-lg space-y-8">
          <div>
            <p className="font-mono text-xs uppercase tracking-widest text-stamp">Ledger address</p>
            <h1 className="font-display text-2xl font-semibold mt-2">Your UPI IDs</h1>
            <p className="mt-2 text-sm text-ink-text-dim">
              This is what others pay to. Creating one needs a wallet and a verified bank account.
            </p>
          </div>

          {actionError && <p className="font-mono text-xs text-debit">{actionError}</p>}

          {upiIds.length === 0 ? (
            <p className="font-mono text-xs text-ink-text-dim">No UPI IDs yet.</p>
          ) : (
            <div className="space-y-3">
              {upiIds.map((upiId) => (
                <div
                  key={upiId.id}
                  className="rounded-lg bg-paper text-paper-text p-5 flex items-center justify-between gap-4"
                >
                  <p className="font-mono text-base">{upiId.vpa}</p>
                  <div className="flex items-center gap-3 shrink-0">
                    {upiId.isDefault ? (
                      <span className="rounded-full bg-stamp/20 text-stamp px-2 py-0.5 font-mono text-xs">
                        DEFAULT
                      </span>
                    ) : (
                      <button
                        type="button"
                        onClick={() => handleSetDefault(upiId.id)}
                        disabled={busy}
                        className="font-mono text-xs text-credit underline underline-offset-4 disabled:opacity-60"
                      >
                        Set as default
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}

          <form onSubmit={handleCreate} className="space-y-3 border-t border-ink-text/10 pt-6">
            <label htmlFor="username" className="block font-mono text-xs text-ink-text-dim">
              Create a new UPI ID
            </label>
            <div className="flex items-center gap-2">
              <input
                id="username"
                type="text"
                placeholder="yourname"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className={inputClass}
              />
              <span className="font-mono text-sm text-ink-text-dim shrink-0">@upisim</span>
            </div>

            {checking && <p className="font-mono text-xs text-ink-text-dim">Checking…</p>}
            {!checking && availability && (
              <p className={`font-mono text-xs ${availability.available ? 'text-credit' : 'text-debit'}`}>
                {availability.available ? `${availability.vpa} is available` : availability.reason}
              </p>
            )}
            {createStatus.error && <p className="font-mono text-xs text-debit">{createStatus.error}</p>}

            <button
              type="submit"
              disabled={!canSubmit || createStatus.submitting}
              className="rounded-md bg-credit text-ink font-display text-sm font-semibold py-2 px-5 disabled:opacity-60"
            >
              {createStatus.submitting ? 'Creating…' : 'Create UPI ID'}
            </button>
          </form>
        </div>
      </main>
    </div>
  )
}
