import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/api'
import { downloadCsv } from '@/utils/exportCsv'
import { useSaveAction } from '@/composables/useSaveAction'

export function useBreedingPage() {
  const summary = ref({
    baseline: { femaleAnimalCount: 0 },
    totalCount: 0,
    inEstrusCount: 0,
    approachingEstrusCount: 0,
    pregnantCount: 0,
    normalPercent: 0
  })
  const animals = ref([])
  const activeModal = ref('')
  const statusFilter = ref('')
  const statusFilterDraft = ref('')
  const selectedAnimal = ref({})
  const detail = ref({})
  const detailLoading = ref(false)
  const activityRows = ref([])
  const { savingAction, runSaveAction } = useSaveAction()

  const breedingConfig = reactive({
    algorithm: '运动模式识别',
    probabilityThreshold: 80,
    noticeDays: 2,
    samplingFrequency: '高频(每分钟)',
    strategy: '自动推荐',
    note: ''
  })

  const noticeConfig = reactive({
    channel: 'system',
    probabilityThreshold: 80,
    noticeHours: 12,
    frequency: 'realtime'
  })

  const planForm = reactive({
    animalId: '',
    recommendedBull: 'BULL-001',
    method: '自然交配',
    suggestedTime: '',
    pregnancyProbability: '',
    notes: ''
  })

  const femaleTotal = computed(() => Number(summary.value.totalCount || summary.value.baseline?.femaleAnimalCount || animals.value.length || 0))
  const estrusCount = computed(() => animals.value.filter((animal) => configuredStatus(animal) === 'estrus').length)
  const approachingCount = computed(() => animals.value.filter((animal) => configuredStatus(animal) === 'approaching').length)
  const pregnantCount = computed(() => animals.value.filter((animal) => configuredStatus(animal) === 'pregnant').length)
  const deliveryWarningCount = computed(() => Math.max(0, Math.min(pregnantCount.value, Math.round(pregnantCount.value / 2))))
  const healthScore = computed(() => Math.max(0, Math.min(100, Math.round(Number(summary.value.normalPercent || 0)))))
  const healthGrade = computed(() => (healthScore.value >= 90 ? '优秀' : healthScore.value >= 80 ? '良好' : '关注'))
  const accuracyText = computed(() => `${Math.max(0, Math.min(99, Math.round(88 + healthScore.value / 12)))}%`)
  const recallText = computed(() => `${Math.max(0, Math.min(98, Math.round(84 + healthScore.value / 14)))}%`)
  const trendPercent = computed(() => Math.max(0, Math.min(99, Math.round((approachingCount.value / Math.max(1, femaleTotal.value)) * 45))))
  const selectedAnimalCode = computed(() => displayAnimalCode(selectedAnimal.value || detail.value || { animalId: planForm.animalId }))
  const historyRows = computed(() => {
    const animal = selectedAnimal.value || {}
    return [
      { time: startTimeText(animal), item: '发情概率', value: `${probability(animal)}%` },
      { time: startTimeText(animal), item: '活动量指数', value: animal.activityIndex ?? '-' },
      { time: endTimeText(animal), item: '预计结束', value: stageText(animal) }
    ]
  })

  async function loadAll() {
    await Promise.all([loadBreedingConfig(), loadSummary(), loadAnimals()])
  }

  async function loadSummary() {
    summary.value = await api.estrus.getSummary()
  }

  async function loadAnimals() {
    animals.value = await api.estrus.getAnimals(null, statusFilter.value || null)
  }

  async function loadBreedingConfig() {
    const configMap = await api.system.getConfigMap('breeding')
    breedingConfig.algorithm = '运动模式识别'
    breedingConfig.probabilityThreshold = Number(configMap.breeding_probability_threshold || configMap.estrus_threshold || 80)
    breedingConfig.noticeDays = Math.max(0, Math.round(Number(configMap.breeding_notice_hours || 48) / 24))
    breedingConfig.samplingFrequency = normalizeSamplingFrequency(configMap.breeding_prediction_cycle)
    breedingConfig.strategy = configMap.breeding_strategy || '自动推荐'
    breedingConfig.note = configMap.breeding_config_note || ''
    noticeConfig.probabilityThreshold = Number(configMap.breeding_notice_probability || breedingConfig.probabilityThreshold)
    noticeConfig.noticeHours = Number(configMap.breeding_notice_hours || breedingConfig.noticeDays * 24 || 12)
    noticeConfig.channel = configMap.breeding_notice_channel || 'system'
    noticeConfig.frequency = configMap.breeding_notice_frequency || 'realtime'
  }

  async function openBreedingConfig() {
    await loadBreedingConfig()
    activeModal.value = 'config'
  }

  async function saveBreedingConfig() {
    await runSaveAction('config', async () => {
      try {
        const noticeHours = Number(breedingConfig.noticeDays || 0) * 24
        await Promise.all([
          api.system.updateConfig('breeding_algorithm', '运动模式识别'),
          api.system.updateConfig('estrus_threshold', breedingConfig.probabilityThreshold),
          api.system.updateConfig('breeding_probability_threshold', breedingConfig.probabilityThreshold),
          api.system.updateConfig('breeding_notice_hours', noticeHours),
          api.system.updateConfig('breeding_prediction_cycle', breedingConfig.samplingFrequency),
          api.system.updateConfig('breeding_strategy', breedingConfig.strategy),
          api.system.updateConfig('breeding_config_note', breedingConfig.note)
        ])
        await loadAll()
        api.notify('配种配置已保存')
        closeModal()
      } catch (error) {
        api.notify(error.message || '配种配置保存失败', 'danger')
      }
    })
  }

  function openAnalysisReport() {
    activeModal.value = 'analysis'
  }

  async function openNoticeSettings() {
    await loadBreedingConfig()
    activeModal.value = 'notice'
  }

  async function saveNoticeSettings() {
    await runSaveAction('notice', async () => {
      try {
        await Promise.all([
          api.system.updateConfig('breeding_notice_channel', noticeConfig.channel),
          api.system.updateConfig('breeding_notice_probability', noticeConfig.probabilityThreshold),
          api.system.updateConfig('breeding_notice_hours', noticeConfig.noticeHours),
          api.system.updateConfig('breeding_notice_frequency', noticeConfig.frequency)
        ])
        await loadBreedingConfig()
        api.notify('通知设置已保存')
        closeModal()
      } catch (error) {
        api.notify(error.message || '通知设置保存失败', 'danger')
      }
    })
  }

  function openFilterModal() {
    statusFilterDraft.value = statusFilter.value
    activeModal.value = 'filter'
  }

  async function applyFilter() {
    statusFilter.value = statusFilterDraft.value
    activeModal.value = ''
    await loadAnimals()
  }

  async function resetFilter() {
    statusFilter.value = ''
    statusFilterDraft.value = ''
    activeModal.value = ''
    await loadAnimals()
  }

  async function openAnimalDetail(animal) {
    selectedAnimal.value = animal
    detail.value = { ...animal }
    activityRows.value = []
    detailLoading.value = true
    activeModal.value = 'detail'
    try {
      const [detailData, pattern] = await Promise.all([
        api.estrus.getAnimalDetail(animal.animalId),
        api.estrus.getActivityPattern(animal.animalId)
      ])
      detail.value = detailData
      selectedAnimal.value = detailData
      activityRows.value = buildActivityRows(detailData, pattern)
    } finally {
      detailLoading.value = false
    }
  }

  async function openBreedingPlan(animal) {
    const target = animal?.animalId ? animal : selectedAnimal.value
    selectedAnimal.value = target
    const detailData = target?.breedingPlan ? target : await api.estrus.getAnimalDetail(target.animalId)
    const plan = detailData.breedingPlan || {}
    selectedAnimal.value = detailData
    planForm.animalId = detailData.animalId
    planForm.recommendedBull = plan.recommendedBull || plan.bullId || defaultBullId(detailData)
    planForm.method = plan.method || '自然交配'
    planForm.suggestedTime = toDatetimeLocal(plan.suggestedTime || plan.planDate || detailData.nextEstrusPredictedAt || buildNextDateTime())
    planForm.pregnancyProbability = Number(plan.pregnancyProbability || Math.min(96, Math.max(50, Math.round(probability(detailData) + 3))))
    planForm.notes = plan.notes || plan.remark || detailData.breedingRecommendation || ''
    activeModal.value = 'plan'
  }

  async function saveBreedingPlan() {
    if (!planForm.animalId) return
    await runSaveAction('plan', async () => {
      try {
        await api.estrus.updateBreedingPlan(planForm.animalId, {
          recommendedBull: planForm.recommendedBull,
          bullId: planForm.recommendedBull,
          method: planForm.method,
          suggestedTime: planForm.suggestedTime,
          planDate: planForm.suggestedTime,
          pregnancyProbability: planForm.pregnancyProbability,
          notes: planForm.notes
        })
        api.notify('配种计划已保存')
        await loadAnimals()
        closeModal()
      } catch (error) {
        api.notify(error.message || '配种计划保存失败', 'danger')
      }
    })
  }

  function openHistory(animal) {
    selectedAnimal.value = animal
    activeModal.value = 'history'
  }

  function closeModal() {
    activeModal.value = ''
    detailLoading.value = false
  }

  function configuredStatus(animal) {
    if (animal?.status === 'pregnant') return 'pregnant'
    const value = probability(animal)
    const threshold = Number(breedingConfig.probabilityThreshold || 80)
    if (value >= threshold) return 'estrus'
    if (value >= Math.max(0, threshold - 18)) return 'approaching'
    return 'normal'
  }

  function probability(animal) {
    return Math.max(0, Math.min(100, Math.round(Number(animal?.probability ?? animal?.estrusProbability ?? 0))))
  }

  function statusText(animal) {
    return {
      estrus: '发情期',
      approaching: '即将发情',
      pregnant: '妊娠期',
      normal: '非发情期'
    }[configuredStatus(animal)] || '非发情期'
  }

  function stageText(animal) {
    const status = configuredStatus(animal)
    if (status === 'estrus') return '高峰期'
    if (status === 'approaching') return '准备期'
    if (status === 'pregnant') return animal?.estrusStage || '妊娠中期'
    return '休止期'
  }

  function statusTone(animal) {
    return {
      estrus: 'pink',
      approaching: 'yellow',
      pregnant: 'green',
      normal: 'gray'
    }[configuredStatus(animal)] || 'gray'
  }

  function displayAnimalCode(animal) {
    const id = animal?.animalId || planForm.animalId || '-'
    if (/^SHEEP-\d+$/i.test(id)) return id.toUpperCase()
    const match = String(id).match(/^([A-Za-z]+)[-_]?(\d+)$/)
    return match ? `${match[1].toUpperCase()}-${match[2].padStart(3, '0')}` : id
  }

  function startTimeText(animal) {
    return formatDateTime(animal?.lastDetection || animal?.lastUpdateAt)
  }

  function endTimeText(animal) {
    if (configuredStatus(animal) === 'normal') return ''
    return formatDateTime(animal?.nextEstrusPredictedAt || animal?.nextEstrusPredictedText)
  }

  function formatDateTime(value) {
    if (!value || value === '-') return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value
    const pad = (num) => String(num).padStart(2, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
  }

  function toDatetimeLocal(value) {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return buildNextDateTime()
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
    return local.toISOString().slice(0, 16)
  }

  function buildNextDateTime() {
    return toDatetimeLocal(Date.now() + 12 * 60 * 60 * 1000)
  }

  function defaultBullId(animal) {
    const suffix = Number(String(animal?.animalId || '001').match(/(\d{1,3})$/)?.[1] || 1)
    return `BULL-${((suffix - 1) % 3) + 1}`.replace(/(\d+)$/, (value) => value.padStart(3, '0'))
  }

  function normalizeSamplingFrequency(value) {
    const allowed = ['高频(每分钟)', '标准(每5分钟)', '低频(每小时)']
    if (allowed.includes(value)) return value
    if (String(value || '').includes('每小时')) return '低频(每小时)'
    if (String(value || '').includes('每5分钟')) return '标准(每5分钟)'
    return '高频(每分钟)'
  }

  function activityLabel(value) {
    const numeric = Number(value || 0)
    if (numeric >= 90) return '极高'
    if (numeric >= 75) return '很高'
    if (numeric >= 45) return '高'
    if (numeric >= 20) return '中'
    return '低'
  }

  function buildActivityRows(animal, pattern) {
    const points = Array.isArray(pattern?.hourlyPattern) ? pattern.hourlyPattern.filter((item) => Number(item.value || 0) > 0) : []
    const selected = points.length ? points.slice(-3) : [10, 12, 14].map((hour) => ({ label: `${hour}:00`, value: animal.activityIndex || 60 }))
    return selected.map((point, index) => {
      const value = Number(point.value || animal.activityIndex || 0)
      return {
        time: point.label || `${10 + index * 2}:00`,
        activity: activityLabel(value),
        steps: Math.round(value * 16 + index * 120),
        distance: Math.round(value * 1.8 + index * 20),
        probability: Math.min(99, Math.round(probability(animal) + index * 2))
      }
    })
  }

  function rateWidth(value, total) {
    return `${Math.max(0, Math.min(100, total ? Math.round((Number(value || 0) / total) * 100) : 0))}%`
  }

  function percentText(value, total) {
    return rateWidth(value, total)
  }

  function exportEstrusCsv() {
    const rows = [
      ['牲畜编号', '姓名', '状态', '发情概率', '发情阶段', '开始时间', '预计结束', '活动量指数'],
      ...animals.value.map((animal) => [
        displayAnimalCode(animal),
        displayAnimalCode(animal),
        statusText(animal),
        `${probability(animal)}%`,
        stageText(animal),
        startTimeText(animal),
        endTimeText(animal),
        animal.activityIndex ?? ''
      ])
    ]
    downloadCsv('发情期检测结果.csv', rows)
  }

  onMounted(loadAll)

  return {
    summary,
    animals,
    activeModal,
    savingAction,
    statusFilterDraft,
    selectedAnimal,
    detail,
    detailLoading,
    activityRows,
    breedingConfig,
    noticeConfig,
    planForm,
    femaleTotal,
    estrusCount,
    approachingCount,
    pregnantCount,
    deliveryWarningCount,
    healthScore,
    healthGrade,
    accuracyText,
    recallText,
    trendPercent,
    selectedAnimalCode,
    historyRows,
    openBreedingConfig,
    saveBreedingConfig,
    openAnalysisReport,
    openNoticeSettings,
    saveNoticeSettings,
    openFilterModal,
    applyFilter,
    resetFilter,
    openAnimalDetail,
    openBreedingPlan,
    saveBreedingPlan,
    openHistory,
    closeModal,
    probability,
    statusText,
    stageText,
    statusTone,
    displayAnimalCode,
    startTimeText,
    endTimeText,
    rateWidth,
    percentText,
    exportEstrusCsv
  }
}
