import {
  normalizeDashboardAnimalDetail,
  normalizeDashboardCharts,
  normalizeDashboardSummary,
  normalizeFocusAnimal,
  normalizeInsight
} from '../normalizers'

export function createDashboardApi({ request, getFarmId }) {
  return {
    getSummary: async (farmId) => normalizeDashboardSummary(await request(`/dashboard/summary?farmId=${farmId || getFarmId()}`)),
    getCharts: async (farmId) => normalizeDashboardCharts(await request(`/dashboard/charts?farmId=${farmId || getFarmId()}`)),
    getFocusAnimals: async (farmId, limit = 10) => {
      const data = await request(`/dashboard/focus-animals?farmId=${farmId || getFarmId()}&limit=${limit}`)
      return Array.isArray(data) ? data.map(normalizeFocusAnimal) : []
    },
    getHeatmap: (farmId) => request(`/dashboard/heatmap?farmId=${farmId || getFarmId()}`),
    getDistribution: (farmId) => request(`/dashboard/distribution?farmId=${farmId || getFarmId()}`),
    getInsights: async (farmId) => {
      const data = await request(`/dashboard/insights?farmId=${farmId || getFarmId()}`)
      return Array.isArray(data) ? data.map(normalizeInsight) : []
    },
    getAnimalDetail: async (animalId) => normalizeDashboardAnimalDetail(await request(`/dashboard/animals/${animalId}`)),
    getRanking: async (farmId, orderBy, limit = 10) => {
      const data = await request(`/dashboard/ranking?farmId=${farmId || getFarmId()}&orderBy=${orderBy || 'healthScore'}&limit=${limit}`)
      return Array.isArray(data) ? data : []
    }
  }
}
