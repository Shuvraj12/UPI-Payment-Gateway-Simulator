import axios from 'axios'

// VITE_ prefixed env vars are inlined into the client bundle at build time -
// never put anything secret in one. The API base URL is the only thing this
// frontend needs to know about the backend's location.
const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export const api = axios.create({
  baseURL,
  timeout: 8000,
})

// The access token lives in AuthContext's React state, not here - this
// module-level variable is just a sync target so the interceptor below can
// read it without importing React. AuthContext calls setAccessToken()
// whenever its own state changes.
let accessToken = null

export function setAccessToken(token) {
  accessToken = token
}

api.interceptors.request.use((config) => {
  if (accessToken && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

// Not added yet: a response interceptor that silently retries a 401 after
// refreshing. Nothing in Phase 2's own UI calls a protected endpoint (login/
// register/refresh are all public), so there's nothing real to verify that
// logic against yet. Coming in Phase 3 once GET /profile exists.

export default api
