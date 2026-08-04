import { useEffect, useState } from 'react'
import { checkHealth } from '../services/healthService.js'

const STATUS_COPY = {
  checking: { label: 'checking…', stamp: 'CHECKING' },
  verified: { label: 'verified', stamp: 'VERIFIED' },
  offline: { label: 'not reachable', stamp: 'OFFLINE' },
}

export default function LedgerEntry() {
  const [status, setStatus] = useState('checking')
  const [entry, setEntry] = useState(null)

  useEffect(() => {
    let cancelled = false

    checkHealth()
      .then((data) => {
        if (cancelled) return
        setEntry(data)
        setStatus('verified')
      })
      .catch(() => {
        if (cancelled) return
        setStatus('offline')
      })

    return () => {
      cancelled = true
    }
  }, [])

  const stampColor =
    status === 'verified' ? 'text-credit border-credit' :
    status === 'offline' ? 'text-debit border-debit' :
    'text-ink-text-dim border-ink-text-dim'

  return (
    <div className="rounded-lg bg-paper text-paper-text shadow-[0_20px_60px_-25px_rgba(0,0,0,0.6)] overflow-hidden">
      <div className="border-b border-paper-text/10 px-5 py-3 flex items-center justify-between">
        <span className="font-mono text-xs uppercase tracking-widest text-paper-text-dim">
          Ledger entry · 001
        </span>
        <span className="font-mono text-xs text-paper-text-dim">
          {entry?.timestamp ? new Date(entry.timestamp).toLocaleTimeString() : '--:--:--'}
        </span>
      </div>

      <div className="px-5 py-5 flex items-center justify-between gap-4">
        <div className="min-w-0">
          <p className="font-display text-lg font-medium">SYSTEM → BACKEND</p>
          <p className="font-mono text-sm text-paper-text-dim truncate">
            ref: {entry?.reference ?? 'pending…'}
          </p>
        </div>

        <div
          className={`shrink-0 rounded-full border-2 px-4 py-2 font-display text-xs font-semibold tracking-wider -rotate-6 transition-all duration-300 ${stampColor} ${
            status === 'checking' ? 'scale-90 opacity-60' : 'scale-100 opacity-100'
          }`}
        >
          {STATUS_COPY[status].stamp}
        </div>
      </div>

      {status === 'offline' && (
        <p className="px-5 pb-4 -mt-2 font-mono text-xs text-debit">
          Start the Spring Boot server on :8080 (or check VITE_API_BASE_URL in .env).
        </p>
      )}
    </div>
  )
}
