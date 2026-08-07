import { useEffect, useState, useCallback } from 'react'
import Header from '../components/Header.jsx'
import TransactionRow from '../components/TransactionRow.jsx'
import * as walletService from '../services/walletService.js'

const inputClass =
  'w-full rounded-md bg-ink-soft border border-ink-text/15 px-3 py-2 font-body text-sm text-ink-text ' +
  'placeholder:text-ink-text-dim/60 focus:outline-none focus:border-credit'

const PAGE_SIZE = 10

export default function Wallet() {
  const [wallet, setWallet] = useState(null)
  const [walletExists, setWalletExists] = useState(null) // null = still checking
  const [creating, setCreating] = useState(false)
  const [loadError, setLoadError] = useState(null)

  const [depositForm, setDepositForm] = useState({ amount: '', description: '' })
  const [depositStatus, setDepositStatus] = useState({ error: null, submitting: false })

  const [freezeSubmitting, setFreezeSubmitting] = useState(false)
  const [freezeError, setFreezeError] = useState(null)

  const [transactions, setTransactions] = useState([])
  const [pageInfo, setPageInfo] = useState({ number: 0, totalPages: 0, totalElements: 0 })

  const loadTransactions = useCallback(async (page = 0) => {
    try {
      const data = await walletService.getTransactions(page, PAGE_SIZE)
      setTransactions(data.content)
      setPageInfo(data.page)
    } catch {
      // Ledger failing to load isn't fatal to the page - the balance/actions above still work.
    }
  }, [])

  const loadWallet = useCallback(async () => {
    try {
      const data = await walletService.getWallet()
      setWallet(data)
      setWalletExists(true)
      loadTransactions(0)
    } catch (err) {
      if (err.response?.status === 404) {
        setWalletExists(false)
      } else {
        setLoadError('Could not load your wallet. Try refreshing the page.')
      }
    }
  }, [loadTransactions])

  useEffect(() => {
    loadWallet()
  }, [loadWallet])

  async function handleCreateWallet() {
    setCreating(true)
    try {
      const data = await walletService.createWallet()
      setWallet(data)
      setWalletExists(true)
      loadTransactions(0)
    } catch {
      setLoadError('Could not open your wallet. Try again.')
    } finally {
      setCreating(false)
    }
  }

  async function handleDeposit(event) {
    event.preventDefault()
    setDepositStatus({ error: null, submitting: true })
    try {
      const data = await walletService.deposit({
        amount: depositForm.amount,
        description: depositForm.description || undefined,
      })
      setWallet(data)
      setDepositForm({ amount: '', description: '' })
      setDepositStatus({ error: null, submitting: false })
      loadTransactions(0)
    } catch (err) {
      setDepositStatus({
        error: err.response?.data?.message ?? 'Deposit failed.',
        submitting: false,
      })
    }
  }

  async function handleFreezeToggle() {
    setFreezeSubmitting(true)
    setFreezeError(null)
    try {
      const data = wallet.frozen ? await walletService.unfreezeWallet() : await walletService.freezeWallet()
      setWallet(data)
    } catch (err) {
      setFreezeError(err.response?.data?.message ?? 'Could not update freeze status.')
    } finally {
      setFreezeSubmitting(false)
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

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-1 px-6 py-14">
        <div className="mx-auto max-w-lg space-y-10">
          <div>
            <p className="font-mono text-xs uppercase tracking-widest text-stamp">Ledger balance</p>
            <h1 className="font-display text-2xl font-semibold mt-2">Your wallet</h1>
          </div>

          {walletExists === false && (
            <div className="rounded-lg bg-paper text-paper-text p-6 text-center">
              <p className="font-display text-lg font-medium mb-2">No wallet yet</p>
              <p className="text-sm text-paper-text-dim mb-5">
                Open one to start simulating deposits and, in later phases, transfers.
              </p>
              <button
                type="button"
                onClick={handleCreateWallet}
                disabled={creating}
                className="rounded-md bg-credit text-ink font-display text-sm font-semibold py-2 px-5 disabled:opacity-60"
              >
                {creating ? 'Opening…' : 'Open your wallet'}
              </button>
            </div>
          )}

          {walletExists && wallet && (
            <>
              <section className="rounded-lg bg-paper text-paper-text p-6">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="font-mono text-xs text-paper-text-dim uppercase tracking-widest">Balance</p>
                    <p className="font-mono text-3xl font-semibold mt-1">
                      &#8377;{Number(wallet.balance).toFixed(2)}
                    </p>
                  </div>
                  {wallet.frozen && (
                    <span className="rounded-full border-2 border-debit text-debit px-3 py-1 font-display text-xs font-semibold -rotate-6">
                      FROZEN
                    </span>
                  )}
                </div>

                <button
                  type="button"
                  onClick={handleFreezeToggle}
                  disabled={freezeSubmitting}
                  className="mt-4 font-mono text-xs text-paper-text-dim underline underline-offset-4 disabled:opacity-60"
                >
                  {freezeSubmitting ? 'Updating…' : wallet.frozen ? 'Unfreeze wallet' : 'Freeze wallet'}
                </button>
                {freezeError && <p className="font-mono text-xs text-debit mt-2">{freezeError}</p>}
              </section>

              <section>
                <p className="font-mono text-xs uppercase tracking-widest text-ink-text-dim mb-4">
                  Simulate a deposit
                </p>
                <form onSubmit={handleDeposit} className="space-y-4">
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
                      value={depositForm.amount}
                      onChange={(e) => setDepositForm((prev) => ({ ...prev, amount: e.target.value }))}
                      className={inputClass}
                    />
                  </div>
                  <div>
                    <label htmlFor="description" className="block font-mono text-xs text-ink-text-dim mb-1">
                      Note (optional)
                    </label>
                    <input
                      id="description"
                      type="text"
                      placeholder="Simulated deposit"
                      value={depositForm.description}
                      onChange={(e) => setDepositForm((prev) => ({ ...prev, description: e.target.value }))}
                      className={inputClass}
                    />
                  </div>

                  {depositStatus.error && <p className="font-mono text-xs text-debit">{depositStatus.error}</p>}

                  <button
                    type="submit"
                    disabled={depositStatus.submitting}
                    className="rounded-md bg-credit text-ink font-display text-sm font-semibold py-2 px-5 disabled:opacity-60"
                  >
                    {depositStatus.submitting ? 'Adding funds…' : 'Add funds (simulated)'}
                  </button>
                  <p className="font-mono text-xs text-ink-text-dim">
                    Test/demo funding only, capped at &#8377;1,00,000 per deposit - not a real payment rail.
                  </p>
                </form>
              </section>

              <section>
                <p className="font-mono text-xs uppercase tracking-widest text-ink-text-dim mb-2">
                  Transaction ledger
                </p>
                {transactions.length === 0 ? (
                  <p className="font-mono text-xs text-ink-text-dim py-4">No entries yet.</p>
                ) : (
                  <div className="border-t border-ink-text/10">
                    {transactions.map((transaction) => (
                      <TransactionRow key={transaction.id} transaction={transaction} />
                    ))}
                  </div>
                )}

                {pageInfo.totalPages > 1 && (
                  <div className="flex items-center justify-between mt-4 font-mono text-xs text-ink-text-dim">
                    <button
                      type="button"
                      onClick={() => loadTransactions(pageInfo.number - 1)}
                      disabled={pageInfo.number === 0}
                      className="disabled:opacity-40"
                    >
                      &larr; Previous
                    </button>
                    <span>
                      Page {pageInfo.number + 1} of {pageInfo.totalPages}
                    </span>
                    <button
                      type="button"
                      onClick={() => loadTransactions(pageInfo.number + 1)}
                      disabled={pageInfo.number + 1 >= pageInfo.totalPages}
                      className="disabled:opacity-40"
                    >
                      Next &rarr;
                    </button>
                  </div>
                )}
              </section>
            </>
          )}
        </div>
      </main>
    </div>
  )
}
