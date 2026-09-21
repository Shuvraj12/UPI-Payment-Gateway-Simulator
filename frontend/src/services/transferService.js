import api from './api.js'

export async function resolveRecipient(vpa) {
  const response = await api.get('/transfers/resolve', { params: { vpa } })
  return response.data.data
}

export async function sendTransfer(payload) {
  const response = await api.post('/transfers', payload)
  return response.data.data
}
