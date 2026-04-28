import {
  normalizeAlertDetail,
  normalizeAlertEvent,
  normalizeAlertRule,
  normalizeAlertSummary,
  normalizePushHistory
} from '../normalizers'

export function createAlertsApi({ request, getFarmId }) {
  return {
    getSummary: async (farmId) => normalizeAlertSummary(await request(`/alerts/summary?farmId=${farmId || getFarmId()}`)),
    getEvents: async (farmId, status, severity, limit = 20) => {
      const data = await request(`/alerts/events?farmId=${farmId || getFarmId()}${status ? `&status=${status}` : ''}${severity ? `&severity=${severity}` : ''}&limit=${limit}`)
      return Array.isArray(data) ? data.map(normalizeAlertEvent) : []
    },
    getEventDetail: async (alertId, farmId) => normalizeAlertDetail(await request(`/alerts/events/${alertId}?farmId=${farmId || getFarmId()}`)),
    acknowledge: (alertId, acknowledgedBy) =>
      request(`/alerts/events/${alertId}/acknowledge?acknowledgedBy=${encodeURIComponent(acknowledgedBy)}`, { method: 'PUT' }),
    resolve: (alertId, resolvedBy, resolutionNote) =>
      request(`/alerts/events/${alertId}/resolve?resolvedBy=${encodeURIComponent(resolvedBy)}${resolutionNote ? `&resolutionNote=${encodeURIComponent(resolutionNote)}` : ''}`, { method: 'PUT' }),
    batchOperation: (alertIds, action, operator) =>
      request(`/alerts/batch?alertIds=${alertIds.join(',')}&action=${action}&operator=${encodeURIComponent(operator)}`, { method: 'POST' }),
    getPushChannels: async (farmId) => {
      const data = await request(`/alerts/push-channels?farmId=${farmId || getFarmId()}`)
      return Array.isArray(data) ? data : []
    },
    updatePushChannel: (channel, config) =>
      request(`/alerts/push-channels/${channel}`, { method: 'PUT', body: JSON.stringify(config) }),
    getPushHistory: async (farmId, limit = 20) => {
      const data = await request(`/alerts/push-history?farmId=${farmId || getFarmId()}&limit=${limit}`)
      return Array.isArray(data) ? data.map(normalizePushHistory) : []
    },
    getRules: async (farmId) => {
      const data = await request(`/alerts/rules?farmId=${farmId || getFarmId()}`)
      return Array.isArray(data) ? data.map(normalizeAlertRule) : []
    },
    createRule: (rule) =>
      request('/alerts/rules', { method: 'POST', body: JSON.stringify(rule) }),
    getTrend: (farmId, days = 7) =>
      request(`/alerts/trend?farmId=${farmId || getFarmId()}&days=${days}`)
  }
}
