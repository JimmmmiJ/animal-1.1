import {
  normalizeAssessment,
  normalizeDiseaseSummary,
  normalizeRiskAnimal,
  normalizeTreatmentPlan
} from '../normalizers'

export function createDiseaseApi({ request, getFarmId }) {
  return {
    getSummary: async (farmId) => normalizeDiseaseSummary(await request(`/disease/summary?farmId=${farmId || getFarmId()}`)),
    getRiskAnimals: async (farmId, riskLevel) => {
      const data = await request(`/disease/risk-animals?farmId=${farmId || getFarmId()}${riskLevel ? `&riskLevel=${riskLevel}` : ''}`)
      return Array.isArray(data) ? data.map(normalizeRiskAnimal) : []
    },
    getAnimalRiskDetail: async (animalId) => normalizeRiskAnimal(await request(`/disease/animals/${animalId}/risk-detail`)),
    getTypeAnalysis: (farmId) => request(`/disease/type-analysis?farmId=${farmId || getFarmId()}`),
    getRiskDistribution: (farmId) => request(`/disease/risk-distribution?farmId=${farmId || getFarmId()}`),
    getHealthEvents: async (farmId, limit = 10) => {
      const data = await request(`/disease/health-events?farmId=${farmId || getFarmId()}&limit=${limit}`)
      return Array.isArray(data) ? data : []
    },
    createTreatmentPlan: async (animalId, planData) =>
      normalizeTreatmentPlan(await request(`/disease/animals/${animalId}/treatment-plan`, { method: 'POST', body: JSON.stringify(planData) })),
    getTreatmentPlan: async (animalId) => normalizeTreatmentPlan(await request(`/disease/animals/${animalId}/treatment-plan`)),
    updateRiskLevel: (animalId, riskLevel) =>
      request(`/disease/animals/${animalId}/risk-level?riskLevel=${riskLevel}`, { method: 'PUT' }),
    assess: async (animalId) => normalizeAssessment(await request(`/disease/animals/${animalId}/assess`, { method: 'POST' }))
  }
}
