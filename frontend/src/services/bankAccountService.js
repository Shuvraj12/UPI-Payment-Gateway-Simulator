import api from './api.js'

export async function getBankAccounts() {
  const response = await api.get('/bank-accounts')
  return response.data.data
}

export async function addBankAccount(payload) {
  const response = await api.post('/bank-accounts', payload)
  return response.data.data
}

export async function verifyBankAccount(accountId) {
  const response = await api.post(`/bank-accounts/${accountId}/verify`)
  return response.data.data
}

export async function setPrimaryBankAccount(accountId) {
  const response = await api.put(`/bank-accounts/${accountId}/primary`)
  return response.data.data
}

export async function deleteBankAccount(accountId) {
  const response = await api.delete(`/bank-accounts/${accountId}`)
  return response.data.data
}
