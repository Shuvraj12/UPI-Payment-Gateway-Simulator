import { Link } from 'react-router-dom'
import Header from '../components/Header.jsx'

export default function NotFound() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <div className="flex-1 flex flex-col items-center justify-center px-6 text-center">
        <p className="font-mono text-xs uppercase tracking-widest text-stamp mb-3">
          No entry found
        </p>
        <h1 className="font-display text-2xl font-semibold">
          This line isn't in the ledger.
        </h1>
        <Link
          to="/"
          className="mt-6 font-mono text-sm text-credit underline underline-offset-4"
        >
          Back to the front page
        </Link>
      </div>
    </div>
  )
}
