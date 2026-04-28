export const API_BASE_URL = '/api'

export function getFarmId() {
  return localStorage.getItem('farmId') || '1'
}

export function notify(message, tone = 'success', duration = 2400) {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('manual-notice', { detail: { message, tone, duration } }))
  }
}

export async function request(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`
  const token = localStorage.getItem('token')
  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` })
    },
    ...options
  }

  try {
    const response = await fetch(url, config)

    if (response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('farmId')
      window.location.href = '/login'
      throw new Error('???????????')
    }

    const text = await response.text()
    const data = text ? JSON.parse(text) : null

    if (!response.ok) {
      throw new Error(data?.message || '????')
    }

    if (data?.success === false) {
      throw new Error(data?.message || '????')
    }

    return data
  } catch (error) {
    console.error('API ????:', error)
    throw error
  }
}
