const TYPE_LABEL = {
  DEPOSIT: 'Simulated deposit',
  TRANSFER: 'Transfer',
  QR_PAYMENT: 'QR payment',
  REQUEST_SETTLEMENT: 'Request settled',
  REFUND: 'Refund',
}

function titleFor(transaction) {
  if (transaction.type === 'TRANSFER' && transaction.counterpartyVpa) {
    return transaction.direction === 'CREDIT'
      ? `Received from ${transaction.counterpartyVpa}`
      : `Sent to ${transaction.counterpartyVpa}`
  }
  return transaction.description || TYPE_LABEL[transaction.type] || transaction.type
}

export default function TransactionRow({ transaction }) {
  const isCredit = transaction.direction === 'CREDIT'
  const amountColor = isCredit ? 'text-credit' : 'text-debit'
  const sign = isCredit ? '+' : '-'

  return (
    <div className="flex items-center justify-between gap-4 py-3 border-b border-ink-text/10 last:border-b-0">
      <div className="min-w-0">
        <p className="font-display text-sm truncate">{titleFor(transaction)}</p>
        <p className="font-mono text-xs text-ink-text-dim truncate">
          {transaction.referenceNumber} &middot; {new Date(transaction.createdAt).toLocaleString()}
        </p>
      </div>
      <div className="text-right shrink-0">
        <p className={`font-mono text-sm font-medium ${amountColor}`}>
          {sign}&#8377;{Number(transaction.amount).toFixed(2)}
        </p>
        <p className="font-mono text-xs text-ink-text-dim">
          bal &#8377;{Number(transaction.balanceAfter).toFixed(2)}
        </p>
      </div>
    </div>
  )
}
