import { getFarmId, notify, request } from './core'
import { createHealthDataApi } from './modules/healthData'
import { createEstrusApi } from './modules/estrus'
import { createDiseaseApi } from './modules/disease'
import { createBehaviorApi } from './modules/behavior'
import { createDashboardApi } from './modules/dashboard'
import { createAlertsApi } from './modules/alerts'
import { createSystemApi } from './modules/system'

const apiContext = { request, getFarmId }

export const api = {
  notify,
  auth: {
    login: (username, password) =>
      request('/system/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
    logout: () => request('/system/logout', { method: 'POST' }),
    getCurrentUser: () => request('/system/current')
  },
  healthData: createHealthDataApi(apiContext),
  estrus: createEstrusApi(apiContext),
  disease: createDiseaseApi(apiContext),
  behavior: createBehaviorApi(apiContext),
  dashboard: createDashboardApi(apiContext),
  alerts: createAlertsApi(apiContext),
  system: createSystemApi({ request })
}

export default api
