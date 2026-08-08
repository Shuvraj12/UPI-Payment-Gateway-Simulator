const BANK_LABELS = {
  STATE_BANK_OF_INDIA: 'State Bank of India',
  HDFC_BANK: 'HDFC Bank',
  ICICI_BANK: 'ICICI Bank',
  AXIS_BANK: 'Axis Bank',
  KOTAK_MAHINDRA_BANK: 'Kotak Mahindra Bank',
  PUNJAB_NATIONAL_BANK: 'Punjab National Bank',
  BANK_OF_BARODA: 'Bank of Baroda',
  CANARA_BANK: 'Canara Bank',
  UNION_BANK_OF_INDIA: 'Union Bank of India',
  INDIAN_BANK: 'Indian Bank',
  IDFC_FIRST_BANK: 'IDFC FIRST Bank',
  YES_BANK: 'Yes Bank',
  INDUSIND_BANK: 'IndusInd Bank',
}

const STATUS_STYLE = {
  VERIFIED: 'text-credit border-credit',
  FAILED: 'text-debit border-debit',
  PENDING: 'text-ink-text-dim border-ink-text-dim',
}

export default function BankAccountCard({ account, onVerify, onSetPrimary, onDelete, busy }) {
  return (
    <div className="rounded-lg bg-paper text-paper-text p-5">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="font-display text-base font-medium">
            {BANK_LABELS[account.bankName] ?? account.bankName}
          </p>
          <p className="font-mono text-sm text-paper-text-dim mt-1">{account.maskedAccountNumber}</p>
          <p className="font-mono text-xs text-paper-text-dim mt-1">
            {account.ifscCode} &middot; {account.accountType === 'SAVINGS' ? 'Savings' : 'Current'}
          </p>
        </div>
        <div className="flex flex-col items-end gap-2 shrink-0">
          {account.primary && (
            <span className="rounded-full bg-stamp/20 text-stamp px-2 py-0.5 font-mono text-xs">PRIMARY</span>
          )}
          <span
            className={`rounded-full border px-2 py-0.5 font-mono text-xs ${STATUS_STYLE[account.verificationStatus]}`}
          >
            {account.verificationStatus}
          </span>
        </div>
      </div>

      <div className="flex items-center gap-4 mt-4 font-mono text-xs">
        {account.verificationStatus !== 'VERIFIED' && (
          <button
            type="button"
            onClick={() => onVerify(account.id)}
            disabled={busy}
            className="text-credit underline underline-offset-4 disabled:opacity-60"
          >
            Verify
          </button>
        )}
        {account.verificationStatus === 'VERIFIED' && !account.primary && (
          <button
            type="button"
            onClick={() => onSetPrimary(account.id)}
            disabled={busy}
            className="text-credit underline underline-offset-4 disabled:opacity-60"
          >
            Set as primary
          </button>
        )}
        <button
          type="button"
          onClick={() => onDelete(account.id)}
          disabled={busy}
          className="text-debit underline underline-offset-4 disabled:opacity-60"
        >
          Remove
        </button>
      </div>
    </div>
  )
}

export { BANK_LABELS }
