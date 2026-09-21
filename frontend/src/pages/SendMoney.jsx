import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import Header from '../components/Header.jsx'
import * as transferService from '../services/transferService.js'

const inputClass =
  'w-full rounded-md bg-ink-soft border border-ink-text/15 px-3 py-2 font-body text-sm text-ink-text ' +
  'placeholder:text-ink-text-dim/60 focus:outline-none focus:border-credit'

export default function SendMoney() {
  const [recipientVpa, setRecipientVpa] = useState('')
  const [resolved, setResolved] = useState(null) // null | { vpa, recipientName } | 'not-found'
  const [resolving, setResolving] = useState(false)
  const latestQuery = useRef('')

  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [sendStatus, setSendStatus] = useState({ error: null, submitting: false })
  const [receipt, setReceipt] = useState(null)

  // Debounced, stale-response-safe recipient lookup - mirrors the UPI ID
  // availability check, but resolving a name instead of a boolean.
  useEffect(() => {
    const trimmed = recipientVpa.trim()
    if (!trimmed) {
      setResolved(null)
      setResolving(false)
      return undefined
    }

    setResolving(true)
    latestQuery.current = trimmed

    const timer = setTimeout(() => {
      transferService
        .resolveRecipient(trimmed)
        .then((result) => {
          if (latestQuery.current === trimmed) {
            setResolved(result)
            setResolving(false)
          }
        })
        .catch(() => {
          if (latestQuery.current === trimmed) {
            setResolved('not-found')
            setResolving(false)
          }
        })
    }, 500)

    return () => clearTimeout(timer)
  }, [recipientVpa])

  async function handleSend(event) {
    event.preventDefault()
    setSendStatus({ error: null, submitting: true })
    try {
      const result = await transferService.sendTransfer({
        recipientVpa: recipientVpa.trim(),
        amount,
        note: note || undefined,
        // Fresh per submit attempt. The submit button is disabled while
        // submitting, so this isn't relied on to stop a double-click - it's
        // the backend's own guarantee against a network-level retry
        // resending the same request twice.
        idempotencyKey: crypto.randomUUID(),
      })
      setReceipt(result)
    } catch (err) {
      setSendStatus({
        error: err.response?.data?.message ?? 'Transfer failed.',
        submitting: false,
      })
    }
  }

  function handleSendAnother() {
    setReceipt(null)
    setRecipientVpa('')
    setResolved(null)
    setAmount('')
    setNote('')
    setSendStatus({ error: null, submitting: false })
  }

  const canSend =
    resolved && resolved !== 'not-found' && Number(amount) > 0 && !resolving && !sendStatus.submitting

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-1 px-6 py-14">
        <div className="mx-auto max-w-lg space-y-8">
          <div>
            <p className="font-mono text-xs uppercase tracking-widest text-stamp">New ledger entry</p>
            <h1 className="font-display text-2xl font-semibold mt-2">Send money</h1>
          </div>

          {receipt ? (
            <div className="rounded-lg bg-paper text-paper-text p-6 space-y-4">
              <div className="flex items-center justify-between">
                <span className="rounded-full border-2 border-credit text-credit px-3 py-1 font-display text-xs font-semibold -rotate-6">
                  SENT
                </span>
                <span className="font-mono text-xs text-paper-text-dim">
                  {new Date(receipt.createdAt).toLocaleString()}
                </span>
              </div>
              <div>
                <p className="font-mono text-xs text-paper-text-dim">Sent to</p>
                <p className="font-mono text-base">{receipt.recipientVpa}</p>
              </div>
              <div>
                <p className="font-mono text-xs text-paper-text-dim">Amount</p>
                <p className="font-mono text-xl font-semibold">&#8377;{Number(receipt.amount).toFixed(2)}</p>
              </div>
              <div>
                <p className="font-mono text-xs text-paper-text-dim">Reference</p>
                <p className="font-mono text-sm">{receipt.referenceNumber}</p>
              </div>
              <div>
                <p className="font-mono text-xs text-paper-text-dim">Your new balance</p>
                <p className="font-mono text-sm">&#8377;{Number(receipt.senderBalanceAfter).toFixed(2)}</p>
              </div>
              <div className="flex items-center gap-4 pt-2">
                <button
                  type="button"
                  onClick={handleSendAnother}
                  className="font-mono text-xs text-credit underline underline-offset-4"
                >
                  Send another
                </button>
                <Link to="/wallet" className="font-mono text-xs text-paper-text-dim underline underline-offset-4">
                  View wallet
                </Link>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSend} className="space-y-5">
              <div>
                <label htmlFor="recipientVpa" className="block font-mono text-xs text-ink-text-dim mb-1">
                  Pay to (UPI ID)
                </label>
                <input
                  id="recipientVpa"
                  type="text"
                  placeholder="name@upisim"
                  required
                  value={recipientVpa}
                  onChange={(e) => setRecipientVpa(e.target.value)}
                  className={inputClass}
                />
                {resolving && <p className="font-mono text-xs text-ink-text-dim mt-1">Looking up…</p>}
                {!resolving && resolved === 'not-found' && (
                  <p className="font-mono text-xs text-debit mt-1">No account found for this UPI ID.</p>
                )}
                {!resolving && resolved && resolved !== 'not-found' && (
                  <p className="font-mono text-xs text-credit mt-1">Paying {resolved.recipientName}</p>
                )}
              </div>

              <div>
                <label htmlFor="amount" className="block font-mono text-xs text-ink-text-dim mb-1">
                  Amount (&#8377;)
                </label>
                <input
                  id="amount"
                  type="number"
                  step="0.01"
                  min="0.01"
                  max="100000"
                  required
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className={inputClass}
                />
              </div>

              <div>
                <label htmlFor="note" className="block font-mono text-xs text-ink-text-dim mb-1">
                  Note (optional)
                </label>
                <input
                  id="note"
                  type="text"
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  className={inputClass}
                />
              </div>

              {sendStatus.error && <p className="font-mono text-xs text-debit">{sendStatus.error}</p>}

              <button
                type="submit"
                disabled={!canSend}
                className="rounded-md bg-credit text-ink font-display text-sm font-semibold py-2.5 px-5 disabled:opacity-60"
              >
                {sendStatus.submitting ? 'Sending…' : 'Send'}
              </button>
              <p className="font-mono text-xs text-ink-text-dim">
                Capped at &#8377;1,00,000 per transfer and &#8377;2,00,000 per day.
              </p>
            </form>
          )}
        </div>
      </main>
    </div>
  )
}
