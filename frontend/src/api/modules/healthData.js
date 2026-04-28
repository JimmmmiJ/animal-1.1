import {
  normalizeActivityStats,
  normalizeDevice,
  normalizeHealthDataSummary,
  normalizeTemperatureTrend
} from '../normalizers'

export function createHealthDataApi({ request, getFarmId }) {
  return {
    getSummary: async (farmId) => normalizeHealthDataSummary(await request(`/health-data/summary?farmId=${farmId || getFarmId()}`)),
    getDevices: async (farmId, status) => {
      const data = await request(`/health-data/devices?farmId=${farmId || getFarmId()}${status ? `&status=${status}` : ''}`)
      return Array.isArray(data) ? data.map(normalizeDevice) : []
    },
    getDeviceDetail: async (deviceId) => normalizeDevice(await request(`/health-data/devices/${deviceId}`)),
    updateDeviceConfig: (deviceId, config) =>
      request(`/health-data/devices/${deviceId}/config`, { method: 'PUT', body: JSON.stringify(config) }),
    getDeviceLogs: async (deviceId, limit = 20) => {
      const data = await request(`/health-data/devices/${deviceId}/logs?limit=${limit}`)
      return Array.isArray(data) ? data : []
    },
    getTemperatureTrend: async (animalId, timeRange) =>
      normalizeTemperatureTrend(await request(`/health-data/animals/${animalId}/temperature-trend?timeRange=${timeRange || '24h'}`)),
    getActivityStats: async (animalId, timeRange) =>
      normalizeActivityStats(await request(`/health-data/animals/${animalId}/activity-stats?timeRange=${timeRange || '24h'}`))
  }
}
