import { useEffect, useState } from 'react'
import Header from '../components/Header.jsx'
import BankAccountCard, { BANK_LABELS } from '../components/BankAccountCard.jsx'
import * as bankAccountService from '../services/bankAccountService.js'

const inputClass =
  'w-full rounded-md bg-ink-soft border border-ink-text/15 px-3 py-2 font-body text-sm text-ink-text ' +
  'placeholder:text-ink-text-dim/60 focus:outline-none focus:border-credit'

const EMPTY_FORM = { accountHolderName: '', bankName: '', accountNumber: '', ifscCode: '', accountType: 'SAVINGS' }

export default function BankAccounts() {
  const [accounts, setAccounts] = useState([])
  const [loadError, setLoadError] = useState(null)

  const [showAddForm, setShowAddForm] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [addStatus, setAddStatus] = useState({ error: null, submitting: false })

  const [actionError, setActionError] = useState(null)
  const [busy, setBusy] = useState(false)

  async function loadAccounts() {
    try {
      const data = await bankAccountService.getBankAccounts()
      setAccounts(data)
    } catch {
      setLoadError('Could not load your bank accounts. Try refreshing the page.')
    }
  }

  useEffect(() => {
    loadAccounts()
  }, [])

  async function handleAddSubmit(event) {
    event.preventDefault()
    setAddStatus({ error: null, submitting: true })
    try {
      await bankAccountService.addBankAccount(form)
      setForm(EMPTY_FORM)
      setShowAddForm(false)
      setAddStatus({ error: null, submitting: false })
      loadAccounts()
    } catch (err) {
      setAddStatus({
        error: err.response?.data?.message ?? 'Could not add that account.',
        submitting: false,
      })
    }
  }

  async function handleVerify(accountId) {
    setBusy(true)
    setActionError(null)
    try {
      await bankAccountService.verifyBankAccount(accountId)
      loadAccounts()
    } catch (err) {
      setActionError(err.response?.data?.message ?? 'Verification could not be attempted.')
    } finally {
      setBusy(false)
    }
  }

  async function handleSetPrimary(accountId) {
    setBusy(true)
    setActionError(null)
    try {
      await bankAccountService.setPrimaryBankAccount(accountId)
      loadAccounts()
    } catch (err) {
      setActionError(err.response?.data?.message ?? 'Could not update the primary account.')
    } finally {
      setBusy(false)
    }
  }

  async function handleDelete(accountId) {
    setBusy(true)
    setActionError(null)
    try {
      await bankAccountService.deleteBankAccount(accountId)
      loadAccounts()
    } catch (err) {
      setActionError(err.response?.data?.message ?? 'Could not remove that account.')
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

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-1 px-6 py-14">
        <div className="mx-auto max-w-lg space-y-8">
          <div>
            <p className="font-mono text-xs uppercase tracking-widest text-stamp">Linked accounts</p>
            <h1 className="font-display text-2xl font-semibold mt-2">Your bank accounts</h1>
            <p className="mt-2 text-sm text-ink-text-dim">
              Shown here the way real UPI apps show them - these don't move wallet money yet, that stays
              wallet-to-wallet starting Phase 7.
            </p>
          </div>

          {actionError && <p className="font-mono text-xs text-debit">{actionError}</p>}

          {accounts.length === 0 ? (
            <p className="font-mono text-xs text-ink-text-dim">No bank accounts linked yet.</p>
          ) : (
            <div className="space-y-3">
              {accounts.map((account) => (
                <BankAccountCard
                  key={account.id}
                  account={account}
                  onVerify={handleVerify}
                  onSetPrimary={handleSetPrimary}
                  onDelete={handleDelete}
                  busy={busy}
                />
              ))}
            </div>
          )}

          {!showAddForm ? (
            <button
              type="button"
              onClick={() => setShowAddForm(true)}
              className="font-mono text-xs text-credit underline underline-offset-4"
            >
              + Add bank account
            </button>
          ) : (
            <form onSubmit={handleAddSubmit} className="space-y-4 border-t border-ink-text/10 pt-6">
              <div>
                <label htmlFor="accountHolderName" className="block font-mono text-xs text-ink-text-dim mb-1">
                  Account holder name
                </label>
                <input
                  id="accountHolderName"
                  type="text"
                  required
                  value={form.accountHolderName}
                  onChange={(e) => setForm((prev) => ({ ...prev, accountHolderName: e.target.value }))}
                  className={inputClass}
                />
              </div>

              <div>
                <label htmlFor="bankName" className="block font-mono text-xs text-ink-text-dim mb-1">
                  Bank
                </label>
                <select
                  id="bankName"
                  required
                  value={form.bankName}
                  onChange={(e) => setForm((prev) => ({ ...prev, bankName: e.target.value }))}
                  className={inputClass}
                >
                  <option value="" disabled>
                    Select a bank
                  </option>
                  {Object.entries(BANK_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="accountNumber" className="block font-mono text-xs text-ink-text-dim mb-1">
                  Account number
                </label>
                <input
                  id="accountNumber"
                  type="text"
                  inputMode="numeric"
                  required
                  placeholder="9-18 digits"
                  value={form.accountNumber}
                  onChange={(e) => setForm((prev) => ({ ...prev, accountNumber: e.target.value }))}
                  className={inputClass}
                />
              </div>

              <div>
                <label htmlFor="ifscCode" className="block font-mono text-xs text-ink-text-dim mb-1">
                  IFSC code
                </label>
                <input
                  id="ifscCode"
                  type="text"
                  required
                  placeholder="e.g. HDFC0001234"
                  value={form.ifscCode}
                  onChange={(e) => setForm((prev) => ({ ...prev, ifscCode: e.target.value.toUpperCase() }))}
                  className={inputClass}
                />
              </div>

              <div>
                <label htmlFor="accountType" className="block font-mono text-xs text-ink-text-dim mb-1">
                  Account type
                </label>
                <select
                  id="accountType"
                  required
                  value={form.accountType}
                  onChange={(e) => setForm((prev) => ({ ...prev, accountType: e.target.value }))}
                  className={inputClass}
                >
                  <option value="SAVINGS">Savings</option>
                  <option value="CURRENT">Current</option>
                </select>
              </div>

              {addStatus.error && <p className="font-mono text-xs text-debit">{addStatus.error}</p>}

              <div className="flex items-center gap-4">
                <button
                  type="submit"
                  disabled={addStatus.submitting}
                  className="rounded-md bg-credit text-ink font-display text-sm font-semibold py-2 px-5 disabled:opacity-60"
                >
                  {addStatus.submitting ? 'Adding…' : 'Add account'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setShowAddForm(false)
                    setForm(EMPTY_FORM)
                    setAddStatus({ error: null, submitting: false })
                  }}
                  className="font-mono text-xs text-ink-text-dim"
                >
                  Cancel
                </button>
              </div>
            </form>
          )}
        </div>
      </main>
    </div>
  )
}
