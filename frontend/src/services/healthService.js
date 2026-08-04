import api from './api.js'

/**
 * Calls GET /health on the backend. Used by the homepage's live ledger
 * entry to prove the frontend and backend can actually reach each other.
 */
export async function checkHealth() {
  const response = await api.get('/health')
  return response.data.data
}
