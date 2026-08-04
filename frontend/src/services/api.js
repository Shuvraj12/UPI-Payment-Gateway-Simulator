import axios from 'axios'

// VITE_ prefixed env vars are inlined into the client bundle at build time -
// never put anything secret in one. The API base URL is the only thing this
// frontend needs to know about the backend's location.
const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export const api = axios.create({
  baseURL,
  timeout: 8000,
})

// Placeholder for Phase 2: once login exists, an interceptor here will
// attach `Authorization: Bearer <token>` from wherever the access token
// ends up living (likely an AuthContext, not localStorage - see README).
// api.interceptors.request.use((config) => { ... })

export default api
