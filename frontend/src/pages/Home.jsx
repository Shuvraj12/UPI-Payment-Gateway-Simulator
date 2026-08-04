import LedgerEntry from '../components/LedgerEntry.jsx'
import RoadmapList from '../components/RoadmapList.jsx'

export default function Home() {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b border-ink-text/10 px-6 py-4 flex items-center justify-between">
        <span className="font-display text-sm font-semibold tracking-widest uppercase">
          UPI Payment Gateway Simulator
        </span>
        <span className="font-mono text-xs text-ink-text-dim">Phase 01 / 14</span>
      </header>

      <main className="flex-1 px-6 py-14 sm:py-20">
        <div className="mx-auto max-w-xl">
          <h1 className="font-display text-3xl sm:text-4xl font-semibold leading-tight">
            Every transfer starts as an entry.
          </h1>
          <p className="mt-4 text-ink-text-dim leading-relaxed">
            This is Phase 1: the frontend and backend can now talk to each
            other. No wallets, transfers, or logins yet — just the ledger
            opening its first page.
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
