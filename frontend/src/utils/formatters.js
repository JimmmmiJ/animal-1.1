export function metricValue(value, unit = '') {
  return value === null || value === undefined || value === '' ? '-' : `${value}${unit}`
}

export function cleanText(value, fallback = '-') {
  if (value === null || value === undefined || value === '') return fallback
  const text = String(value).trim()
  return text.includes('?') || /[�]|[鐢绯鏉寮勫鍛瑙庣]/.test(text) ? fallback : text
}

export function formatAnimalId(value) {
  const text = String(value || '-')
  const match = text.match(/^SHEEP-?0*(\d+)$/i)
  return match ? `SHEEP-${match[1].padStart(3, '0')}` : text
}

export function formatDateTime(value) {
  return compactDateTime(value)
}

export function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10)
  return date.toISOString().slice(0, 10)
}

export function compactDateTime(value) {
  if (!value || value === '-') return '-'
  const date = parseDate(value)
  if (!date) return value
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function clampPercent(value) {
  return Math.max(0, Math.min(100, Math.round(Number(value || 0))))
}

export function round1(value) {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? Math.round(numeric * 10) / 10 : 0
}

function parseDate(value) {
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  const direct = new Date(value)
  if (!Number.isNaN(direct.getTime())) return direct
  const normalized = String(value).replace(/-/g, '/')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function pad(value) {
  return String(value).padStart(2, '0')
}
