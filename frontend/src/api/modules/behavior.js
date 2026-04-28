import {
  normalizeBehaviorReport,
  normalizeBehaviorSummary,
  normalizeSheepBehavior
} from '../normalizers'

export function createBehaviorApi({ request, getFarmId }) {
  return {
    getSummary: async (farmId) => normalizeBehaviorSummary(await request(`/behavior/summary?farmId=${farmId || getFarmId()}`)),
    getSheepList: async (farmId, status) => {
      const data = await request(`/behavior/sheep-list?farmId=${farmId || getFarmId()}${status ? `&status=${status}` : ''}`)
      return Array.isArray(data) ? data.map(normalizeSheepBehavior) : []
    },
    getSheepDetail: async (animalId) => normalizeSheepBehavior(await request(`/behavior/sheep/${animalId}`)),
    get24HourPattern: async (farmId) => {
      const data = await request(`/behavior/24hour-pattern?farmId=${farmId || getFarmId()}`)
      return Array.isArray(data) ? data : []
    },
    getEfficiencyTrend: async (farmId, days = 7) => {
      const data = await request(`/behavior/efficiency-trend?farmId=${farmId || getFarmId()}&days=${days}`)
      return Array.isArray(data) ? data : []
    },
    getAbnormalEvents: async (farmId, limit = 10) => {
      const data = await request(`/behavior/abnormal-events?farmId=${farmId || getFarmId()}&limit=${limit}`)
      return Array.isArray(data) ? data : []
    },
    saveDietPlan: (animalId, dietPlan) =>
      request(`/behavior/sheep/${animalId}/diet-plan`, { method: 'POST', body: JSON.stringify(dietPlan) }),
    getNutritionAdvice: async (farmId) => {
      const data = await request(`/behavior/nutrition-advice?farmId=${farmId || getFarmId()}`)
      return Array.isArray(data) ? data : []
    },
    getAnalysisReport: async (farmId) => normalizeBehaviorReport(await request(`/behavior/analysis-report?farmId=${farmId || getFarmId()}`))
  }
}
