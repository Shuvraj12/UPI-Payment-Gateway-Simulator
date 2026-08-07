import api from './api.js'

export async function getProfile() {
  const response = await api.get('/profile')
  return response.data.data
}

export async function updateProfile(payload) {
  const response = await api.put('/profile', payload)
  return response.data.data
}

export async function changePassword(payload) {
  const response = await api.put('/profile/password', payload)
  return response.data.data
}

export async function uploadProfilePicture(file) {
  const formData = new FormData()
  formData.append('file', file)
  // No explicit Content-Type here - axios detects the FormData instance and
  // sets 'multipart/form-data; boundary=...' itself. Setting it by hand
  // would omit the boundary and the server couldn't parse the body at all.
  const response = await api.post('/profile/picture', formData)
  return response.data.data
}

export async function deleteAccount(password) {
  const response = await api.delete('/profile', { data: { password } })
  return response.data.data
}
