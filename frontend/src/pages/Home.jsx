import { useAuth } from '../context/AuthContext.jsx'
import Header from '../components/Header.jsx'
import LedgerEntry from '../components/LedgerEntry.jsx'
import RoadmapList from '../components/RoadmapList.jsx'

export default function Home() {
  const { user, isAuthenticated } = useAuth()

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <main className="flex-1 px-6 py-14 sm:py-20">
        <div className="mx-auto max-w-xl">
          <span className="font-mono text-xs text-stamp uppercase tracking-widest">Phase 06 / 14</span>

          <h1 className="font-display text-3xl sm:text-4xl font-semibold leading-tight mt-2">
            {isAuthenticated
              ? `Welcome back, ${user?.fullName?.split(' ')[0] ?? ''}.`
              : 'Every transfer starts as an entry.'}
          </h1>
          <p className="mt-4 text-ink-text-dim leading-relaxed">
            {isAuthenticated
              ? 'Your account is open and your session survives a page refresh. Wallets, transfers, and the rest of the ledger arrive in the phases ahead.'
              : 'Accounts are real now. Register, log in, and your session survives a page refresh — no wallets or transfers yet.'}
          </p>

          <div className="mt-10">
            <LedgerEntry />
          </div>

          <div className="mt-14">
            <RoadmapList />
          </div>
        </div>
      </main>

      <footer className="border-t border-ink-text/10 px-6 py-4">
        <p className="font-mono text-xs text-ink-text-dim">
          Simulated ledger, not a real payment system. Built in the open, phase by phase.
        </p>
      </footer>
    </div>
  )
}
