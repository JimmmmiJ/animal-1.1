import {
  normalizeActivityPattern,
  normalizeEstrusAnimal,
  normalizeEstrusSummary,
  normalizeProbabilityTrend
} from '../normalizers'

export function createEstrusApi({ request, getFarmId }) {
  return {
    getSummary: async (farmId) => normalizeEstrusSummary(await request(`/estrus/summary?farmId=${farmId || getFarmId()}`)),
    getAnimals: async (farmId, status) => {
      const data = await request(`/estrus/animals?farmId=${farmId || getFarmId()}${status ? `&status=${status}` : ''}`)
      return Array.isArray(data) ? data.map(normalizeEstrusAnimal) : []
    },
    getAnimalDetail: async (animalId) => normalizeEstrusAnimal(await request(`/estrus/animals/${animalId}`)),
    getActivityPattern: async (animalId) => normalizeActivityPattern(await request(`/estrus/animals/${animalId}/activity-pattern`)),
    getProbabilityTrend: async (animalId, days = 7) =>
      normalizeProbabilityTrend(await request(`/estrus/animals/${animalId}/probability-trend?days=${days}`)),
    getAlertEvents: async (farmId, limit = 10) => {
      const data = await request(`/estrus/alert-events?farmId=${farmId || getFarmId()}&limit=${limit}`)
      return Array.isArray(data) ? data : []
    },
    updateBreedingPlan: (animalId, planData) =>
      request(`/estrus/animals/${animalId}/breeding-plan`, { method: 'POST', body: JSON.stringify(planData) })
  }
}
