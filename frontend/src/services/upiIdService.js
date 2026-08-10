import api from './api.js'

export async function getUpiIds() {
  const response = await api.get('/upi-ids')
  return response.data.data
}

export async function createUpiId(username) {
  const response = await api.post('/upi-ids', { username })
  return response.data.data
}

export async function checkAvailability(username) {
  const response = await api.get('/upi-ids/availability', { params: { username } })
  return response.data.data
}

export async function setDefaultUpiId(upiIdId) {
  const response = await api.put(`/upi-ids/${upiIdId}/default`)
  return response.data.data
}
