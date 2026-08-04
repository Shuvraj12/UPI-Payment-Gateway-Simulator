const UPCOMING = [
  { phase: '02', label: 'Authentication', detail: 'register, login, refresh tokens' },
  { phase: '04', label: 'Wallets', detail: 'balance, ledger, freeze' },
  { phase: '07', label: 'Transfers', detail: 'send, receive, status' },
  { phase: '08', label: 'QR payments', detail: 'generate, scan, pay' },
  { phase: '09', label: 'Money requests', detail: 'create, accept, reject' },
  { phase: '11', label: 'Analytics', detail: 'spend trends, top recipients' },
  { phase: '12', label: 'Admin console', detail: 'users, freezes, statistics' },
]

export default function RoadmapList() {
  return (
    <div>
      <p className="font-mono text-xs uppercase tracking-widest text-ink-text-dim mb-3">
        What the ledger will track
      </p>
      <ul className="divide-y divide-ink-text/10 border-t border-b border-ink-text/10">
        {UPCOMING.map((item) => (
          <li key={item.phase} className="flex items-baseline gap-4 py-3">
            <span className="font-mono text-xs text-stamp shrink-0">
              PH.{item.phase}
            </span>
            <span className="font-display text-sm font-medium">{item.label}</span>
            <span className="font-mono text-xs text-ink-text-dim truncate hidden sm:inline">
              {item.detail}
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
