import { configsToMap, normalizeSystemConfig } from '../normalizers'

export function createSystemApi({ request }) {
  const systemApi = {
    getUsers: (farmId, keyword) =>
      request(`/system/users?farmId=${farmId || ''}${keyword ? `&keyword=${keyword}` : ''}`),
    getUserDetail: (userId) => request(`/system/users/${userId}`),
    createUser: (userData) =>
      request('/system/users', { method: 'POST', body: JSON.stringify(userData) }),
    updateUser: (userId, userData) =>
      request(`/system/users/${userId}`, { method: 'PUT', body: JSON.stringify(userData) }),
    deleteUser: (userId) =>
      request(`/system/users/${userId}`, { method: 'DELETE' }),
    resetPassword: (userId) =>
      request(`/system/users/${userId}/reset-password`, { method: 'POST' }),
    getRoles: () => request('/system/roles'),
    getPermissions: () => request('/system/permissions'),
    getConfig: async (category) => {
      const data = await request(`/system/config${category ? `?category=${category}` : ''}`)
      return Array.isArray(data) ? data.map(normalizeSystemConfig) : []
    },
    getConfigMap: async (category) => configsToMap(await systemApi.getConfig(category)),
    updateConfig: (key, value) =>
      request(`/system/config/${key}?value=${encodeURIComponent(value)}`, { method: 'PUT' }),
    getAuditLogs: (farmId, operationType, limit = 50) =>
      request(`/system/audit-logs?farmId=${farmId || ''}${operationType ? `&operationType=${operationType}` : ''}&limit=${limit}`),
    getStats: () => request('/system/stats')
  }

  return systemApi
}
