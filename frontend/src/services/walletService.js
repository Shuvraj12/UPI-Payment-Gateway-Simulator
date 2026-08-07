import api from './api.js'

export async function getWallet() {
  const response = await api.get('/wallet')
  return response.data.data
}

export async function createWallet() {
  const response = await api.post('/wallet')
  return response.data.data
}

export async function deposit(payload) {
  const response = await api.post('/wallet/deposit', payload)
  return response.data.data
}

export async function freezeWallet() {
  const response = await api.put('/wallet/freeze')
  return response.data.data
}

export async function unfreezeWallet() {
  const response = await api.put('/wallet/unfreeze')
  return response.data.data
}

// Returns the PagedModel shape as-is: { content: [...], page: { size, totalElements, totalPages, number } }
export async function getTransactions(page = 0, size = 10) {
  const response = await api.get('/wallet/transactions', { params: { page, size } })
  return response.data.data
}
