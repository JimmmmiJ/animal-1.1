const riskLevelTextMap = {
  high: '高风险',
  medium: '中风险',
  low: '低风险',
  normal: '健康'
}

const estrusStatusTextMap = {
  estrus: '发情中',
  approaching: '临近发情',
  pregnant: '已怀孕',
  normal: '正常母畜'
}

const behaviorStatusTextMap = {
  normal: '正常',
  warning: '预警',
  abnormal: '异常'
}

const alertTypeTextMap = {
  device: '设备告警',
  behavior: '行为告警',
  health: '健康告警',
  estrus: '发情提醒'
}

const alertSeverityTextMap = {
  high: '高风险',
  medium: '中风险',
  low: '低风险'
}

const alertStatusTextMap = {
  pending: '待处理',
  acknowledged: '已确认',
  resolved: '已解决'
}

export function createEmptyBaseline() {
  return {
    farmId: null,
    snapshotAt: null,
    totalAnimals: 0,
    totalDevices: 0,
    femaleAnimalCount: 0,
    onlineDevices: 0,
    offlineDevices: 0,
    faultDevices: 0,
    abnormalBehaviorAnimalCount: 0,
    riskAnimalCount: 0
  }
}

export function normalizeBaseline(baseline) {
  return {
    ...createEmptyBaseline(),
    ...(baseline || {})
  }
}

export function formatDateTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

export function round1(value) {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? Math.round(numeric * 10) / 10 : 0
}

export function activityTextFromValue(value) {
  const numeric = Number(value || 0)
  if (numeric >= 80) return '高活动'
  if (numeric >= 50) return '中活动'
  if (numeric >= 20) return '低活动'
  return '静息'
}

export function boolFromValue(value, fallback = false) {
  if (typeof value === 'boolean') return value
  if (typeof value === 'string') return value === 'true'
  return fallback
}

export function valueText(value, fallback = '-') {
  if (value === null || value === undefined || value === '') return fallback
  return String(value)
}

export function normalizeSystemConfig(item) {
  return {
    key: item?.key || '',
    value: item?.value ?? '',
    type: item?.type || 'text',
    description: item?.description || '',
    category: item?.category || 'basic'
  }
}

export function configsToMap(configs) {
  return Object.fromEntries((configs || []).map((item) => [item.key, item.value]))
}

export function normalizeHealthDataSummary(raw) {
  const baseline = normalizeBaseline(raw?.baseline)
  return {
    baseline,
    totalDevices: raw?.totalDevices ?? baseline.totalDevices,
    onlineDevices: raw?.onlineDevices ?? baseline.onlineDevices,
    offlineDevices: raw?.offlineDevices ?? baseline.offlineDevices,
    faultDevices: raw?.faultDevices ?? baseline.faultDevices,
    avgBatteryLevel: round1(raw?.avgBatteryLevel),
    devicesWithLowBattery: raw?.devicesWithLowBattery ?? 0,
    devicesWithWeakSignal: raw?.devicesWithWeakSignal ?? 0,
    abnormalDeviceCount:
      raw?.abnormalDeviceCount ??
      Number(raw?.offlineDevices ?? baseline.offlineDevices ?? 0) +
        Number(raw?.faultDevices ?? baseline.faultDevices ?? 0) +
        Number(raw?.devicesWithLowBattery ?? 0) +
        Number(raw?.devicesWithWeakSignal ?? 0),
    avgHealthScore: round1(raw?.avgHealthScore),
    healthyAnimals: raw?.healthyAnimals ?? 0,
    dataUpdateFrequency: raw?.dataUpdateFrequency || '-',
    todayDataCompletionRate: round1(raw?.todayDataCompletionRate)
  }
}

export function normalizeDevice(item) {
  return {
    deviceId: item?.deviceId || '-',
    deviceModel: item?.deviceModel || '-',
    deviceSn: item?.deviceSn || '-',
    farmId: item?.farmId ?? null,
    animalId: item?.animalId || '-',
    animalName: item?.animalName || '-',
    breed: item?.breed || '-',
    status: item?.status || 'offline',
    statusText: {
      online: '在线',
      offline: '离线',
      fault: '故障'
    }[item?.status] || '未知',
    currentTemperature: item?.currentTemperature ?? null,
    currentHeartRate: item?.currentHeartRate ?? null,
    currentActivity: item?.currentActivity ?? null,
    batteryLevel: item?.batteryLevel ?? 0,
    signalStrength: item?.signalStrength ?? 0,
    protocol: item?.protocol || 'MQTT',
    firmwareVersion: item?.firmwareVersion || '-',
    uploadIntervalMinutes: item?.uploadIntervalMinutes ?? 60,
    temperatureMinThreshold: item?.temperatureMinThreshold ?? 38,
    temperatureMaxThreshold: item?.temperatureMaxThreshold ?? 41,
    heartRateMinThreshold: item?.heartRateMinThreshold ?? 60,
    heartRateMaxThreshold: item?.heartRateMaxThreshold ?? 120,
    lowBatteryThreshold: item?.lowBatteryThreshold ?? 20,
    installationLocation: item?.installationLocation || '-',
    note: item?.note || '',
    lastOnlineAt: item?.lastOnlineAt || null,
    lastOnlineText: formatDateTime(item?.lastOnlineAt),
    lastDataUpdateAt: item?.lastDataUpdateAt || null,
    lastDataUpdateText: formatDateTime(item?.lastDataUpdateAt)
  }
}

export function normalizeTemperatureTrend(raw) {
  return {
    animalId: raw?.animalId || '-',
    trend: Array.isArray(raw?.trend)
      ? raw.trend.map((point) => ({
          label: point?.time || '-',
          value: point?.value ?? null
        }))
      : [],
    avgTemperature: round1(raw?.avgTemperature),
    maxTemperature: round1(raw?.maxTemperature),
    minTemperature: round1(raw?.minTemperature)
  }
}

export function normalizeActivityStats(raw) {
  const distribution = raw?.distribution || {}
  return {
    animalId: raw?.animalId || '-',
    avgActivity: raw?.avgActivity ?? 0,
    maxActivity: raw?.maxActivity ?? 0,
    distribution,
    distributionItems: Object.entries(distribution).map(([label, value]) => ({ label, value })),
    hourlyTrend: Array.isArray(raw?.hourlyTrend)
      ? raw.hourlyTrend.map((point) => ({ label: point?.time || '-', value: point?.value ?? 0 }))
      : []
  }
}

export function normalizeRiskAnimal(item) {
  return {
    animalId: item?.animalId || '-',
    name: item?.name || '-',
    breed: item?.breed || '-',
    age: item?.age ?? null,
    weight: item?.weight ?? null,
    riskLevel: item?.riskLevel || 'normal',
    riskLevelText: riskLevelTextMap[item?.riskLevel] || '健康',
    riskScore: round1(item?.riskScore),
    mainRisk: item?.riskType || '综合风险',
    mainSymptoms: item?.mainSymptoms || '-',
    suggestion: item?.recommendation || item?.mainSymptoms || '-',
    healthScore: round1(item?.healthScore),
    currentTemperature: item?.currentTemperature ?? null,
    currentHeartRate: item?.currentHeartRate ?? null,
    lastCheckTime: item?.lastCheckTime || null,
    lastCheckTimeText: formatDateTime(item?.lastCheckTime),
    lastUpdateAt: item?.lastUpdateAt || null,
    symptomTags: Array.isArray(item?.symptomTags) ? item.symptomTags : [],
    riskBasis: Array.isArray(item?.riskBasis) ? item.riskBasis : [],
    recentDetections: Array.isArray(item?.recentDetections) ? item.recentDetections : [],
    temperatureTrend: Array.isArray(item?.temperatureTrend) ? item.temperatureTrend : [],
    heartRateTrend: Array.isArray(item?.heartRateTrend) ? item.heartRateTrend : [],
    activityTrend: Array.isArray(item?.activityTrend) ? item.activityTrend : []
  }
}

export function normalizeDiseaseSummary(raw) {
  const baseline = normalizeBaseline(raw?.baseline)
  return {
    baseline,
    highRiskCount: raw?.highRiskCount ?? 0,
    mediumRiskCount: raw?.mediumRiskCount ?? 0,
    lowRiskCount: raw?.lowRiskCount ?? 0,
    healthyCount: raw?.healthyCount ?? 0,
    healthyPercent: round1(raw?.healthyPercent),
    alertCount: raw?.alertCount ?? 0,
    modelAccuracy: round1(raw?.modelAccuracy),
    totalAnimals: raw?.totalAnimals ?? baseline.totalAnimals
  }
}

export function normalizeTreatmentPlan(raw) {
  return {
    id: raw?.id ?? null,
    animalId: raw?.animalId || '-',
    animalName: raw?.animalName || '-',
    diagnosis: raw?.diagnosis || '',
    treatmentPlan: raw?.treatmentPlan || '',
    medication: raw?.medication || '',
    dosage: raw?.dosage || '',
    frequency: raw?.frequency || '',
    duration: raw?.duration ?? 7,
    startDate: raw?.startDate || null,
    startDateText: formatDateTime(raw?.startDate),
    endDate: raw?.endDate || null,
    endDateText: formatDateTime(raw?.endDate),
    status: raw?.status || '',
    veterinarian: raw?.veterinarian || '',
    notes: raw?.notes || '',
    records: Array.isArray(raw?.records) ? raw.records : []
  }
}

export function normalizeAssessment(raw) {
  return {
    animalId: raw?.animalId || '-',
    assessmentTime: raw?.assessmentTime || null,
    overallRisk: raw?.overallRisk || 'normal',
    riskScore: round1(raw?.riskScore),
    riskFactors: Array.isArray(raw?.riskFactors) ? raw.riskFactors : [],
    recommendations: Array.isArray(raw?.recommendations) ? raw.recommendations : []
  }
}

export function normalizeEstrusSummary(raw) {
  const baseline = normalizeBaseline(raw?.baseline)
  return {
    baseline,
    totalCount: raw?.totalCount ?? baseline.femaleAnimalCount,
    inEstrusCount: raw?.inEstrusCount ?? 0,
    inEstrusPercent: round1(raw?.inEstrusPercent),
    approachingEstrusCount: raw?.approachingEstrusCount ?? 0,
    approachingEstrusPercent: round1(raw?.approachingEstrusPercent),
    pregnantCount: raw?.pregnantCount ?? 0,
    pregnantPercent: round1(raw?.pregnantPercent),
    normalCount: raw?.normalCount ?? 0,
    normalPercent: round1(raw?.normalPercent)
  }
}

export function normalizeEstrusAnimal(item) {
  const activityIndex = item?.activityIndex ?? 0
  return {
    animalId: item?.animalId || '-',
    name: item?.name || '-',
    breed: item?.breed || '-',
    age: item?.age ?? null,
    weight: item?.weight ?? null,
    status: item?.estrusStatus || 'normal',
    statusText: estrusStatusTextMap[item?.estrusStatus] || '未知',
    probability: round1(item?.estrusProbability),
    estrusStage: item?.estrusStage || '未知',
    activityIndex,
    activityText: activityTextFromValue(activityIndex),
    currentTemperature: item?.currentTemperature ?? null,
    currentHeartRate: item?.currentHeartRate ?? null,
    dailyRuminationTime: item?.dailyRuminationTime ?? null,
    dailyFeedingCount: item?.dailyFeedingCount ?? null,
    suggestion: item?.suggestion || '',
    breedingRecommendation: item?.breedingRecommendation || '',
    breedingPlan: item?.breedingPlan && typeof item.breedingPlan === 'object' ? item.breedingPlan : {},
    recentDetections: Array.isArray(item?.recentDetections) ? item.recentDetections : [],
    lastDetection: formatDateTime(item?.lastUpdateAt),
    nextEstrusPredictedAt: item?.nextEstrusPredictedAt || null,
    nextEstrusPredictedText: formatDateTime(item?.nextEstrusPredictedAt)
  }
}

export function normalizeActivityPattern(raw) {
  return {
    hourlyPattern: Array.isArray(raw?.hourlyPattern) ? raw.hourlyPattern : [],
    dailyPattern: Array.isArray(raw?.dailyPattern) ? raw.dailyPattern : [],
    peakActivityHour: raw?.peakActivityHour ?? 0,
    avgActivityLevel: raw?.avgActivityLevel ?? 0
  }
}

export function normalizeProbabilityTrend(raw) {
  return {
    animalId: raw?.animalId || '-',
    currentProbability: round1(raw?.currentProbability),
    prediction: raw?.prediction || '',
    trend: Array.isArray(raw?.trend) ? raw.trend : []
  }
}

export function normalizeBehaviorSummary(raw) {
  const baseline = normalizeBaseline(raw?.baseline)
  return {
    baseline,
    avgRuminationTime: raw?.avgRuminationTime ?? 0,
    avgFeedingCount: raw?.avgFeedingCount ?? 0,
    abnormalAnimalCount: raw?.abnormalAnimalCount ?? 0,
    totalAnimalCount: raw?.totalAnimalCount ?? baseline.totalAnimals,
    abnormalPercent: round1(raw?.abnormalPercent),
    normalPercent: round1(raw?.normalPercent ?? Math.max(0, 100 - (raw?.abnormalPercent ?? 0))),
    digestiveHealthScore: round1(raw?.digestiveHealthScore)
  }
}

export function normalizeSheepBehavior(item) {
  return {
    animalId: item?.animalId || '-',
    name: item?.name || '-',
    breed: item?.breed || '-',
    age: item?.age ?? null,
    weight: item?.weight ?? null,
    healthScore: round1(item?.healthScore),
    ruminationTime: item?.ruminationTime ?? 0,
    feedingCount: item?.feedingCount ?? 0,
    activityLevel: item?.activityLevel ?? 0,
    restingTime: item?.restingTime ?? 0,
    ruminationEfficiency: item?.ruminationEfficiency ?? 0,
    feedingQuality: item?.feedingQuality ?? 0,
    status: item?.status || 'normal',
    statusText: behaviorStatusTextMap[item?.status] || '未知',
    summary: item?.summary || '',
    lastDietAdjustAt: item?.lastDietAdjustAt || null,
    lastDietAdjustText: formatDateTime(item?.lastDietAdjustAt),
    lastUpdate: item?.lastUpdate || null,
    lastUpdateText: formatDateTime(item?.lastUpdate),
    history: Array.isArray(item?.history) ? item.history : [],
    hourlyPattern: Array.isArray(item?.hourlyPattern) ? item.hourlyPattern : [],
    nutritionAdvice: Array.isArray(item?.nutritionAdvice) ? item.nutritionAdvice : []
  }
}

export function normalizeBehaviorReport(raw) {
  return {
    behaviorPattern: raw?.behaviorPattern || {},
    behaviorPatternItems: Object.entries(raw?.behaviorPattern || {}).map(([label, value]) => ({ label, value })),
    healthTrend: Array.isArray(raw?.healthTrend) ? raw.healthTrend : [],
    analysisSummary: raw?.analysisSummary || '',
    abnormalAnimalCount: raw?.abnormalAnimalCount ?? 0,
    avgRuminationTime: round1(raw?.avgRuminationTime),
    avgFeedingCount: round1(raw?.avgFeedingCount),
    recommendations: Array.isArray(raw?.recommendations) ? raw.recommendations : []
  }
}

export function normalizeDashboardSummary(raw) {
  const baseline = normalizeBaseline(raw?.baseline)
  return {
    baseline,
    totalAnimals: raw?.totalAnimals ?? baseline.totalAnimals,
    healthyCount: raw?.healthyCount ?? 0,
    healthyPercent: round1(raw?.healthyPercent),
    attentionCount: raw?.attentionCount ?? 0,
    alertCount: raw?.alertCount ?? 0,
    highRiskCount: raw?.highRiskCount ?? 0,
    mediumRiskCount: raw?.mediumRiskCount ?? 0,
    inEstrusCount: raw?.inEstrusCount ?? 0,
    avgHealthScore: round1(raw?.avgHealthScore),
    avgTemperature: round1(raw?.avgTemperature),
    avgActivity: raw?.avgActivity ?? 0,
    avgRuminationTime: raw?.avgRuminationTime ?? 0,
    ruminationPassRate: round1(raw?.ruminationPassRate),
    weeklyTrend: Array.isArray(raw?.weeklyTrend) ? raw.weeklyTrend : []
  }
}

const dashboardStatusColorMap = {
  healthy: '#10b981',
  attention: '#f59e0b',
  highRisk: '#ef4444'
}

export function normalizeDashboardCharts(raw) {
  const baseline = normalizeBaseline(raw?.baseline)
  const healthStatusDistribution = Array.isArray(raw?.healthStatusDistribution)
    ? raw.healthStatusDistribution.map((segment) => ({
        key: segment?.key || 'healthy',
        label: segment?.label || '健康',
        value: Number(segment?.value ?? 0),
        color: dashboardStatusColorMap[segment?.key] || '#64748b'
      }))
    : []

  return {
    baseline,
    healthStatusDistribution,
    temperatureTrend: {
      warningThreshold: raw?.temperatureTrend?.warningThreshold ?? 39.5,
      dangerThreshold: raw?.temperatureTrend?.dangerThreshold ?? 40.5,
      points: Array.isArray(raw?.temperatureTrend?.points)
        ? raw.temperatureTrend.points.map((point) => ({
            timeBucket: point?.timeBucket || '-',
            value: point?.value ?? null,
            hasData: Boolean(point?.hasData)
          }))
        : []
    },
    activityHeatmap: {
      timeBuckets: Array.isArray(raw?.activityHeatmap?.timeBuckets) ? raw.activityHeatmap.timeBuckets : [],
      metrics: Array.isArray(raw?.activityHeatmap?.metrics) ? raw.activityHeatmap.metrics : [],
      cells: Array.isArray(raw?.activityHeatmap?.cells) ? raw.activityHeatmap.cells : []
    }
  }
}

export function deriveRiskTypeText(item) {
  if (item?.riskLevel === 'high') return '高风险'
  if (item?.behaviorStatus === 'abnormal') return '行为异常'
  if (item?.estrusStatus === 'estrus') return '发情关注'
  if (item?.riskLevel === 'medium') return '中风险'
  return '需关注'
}

export function normalizeFocusAnimal(item) {
  return {
    animalId: item?.animalId || '-',
    name: item?.name || '-',
    deviceId: item?.deviceId || '-',
    healthScore: round1(item?.healthScore),
    riskTypeText: deriveRiskTypeText(item),
    temperature: item?.currentTemperature ?? null,
    activityText: activityTextFromValue(item?.currentActivity ?? 0),
    riskLevel: item?.riskLevel || 'normal',
    behaviorStatus: item?.behaviorStatus || 'normal',
    estrusStatus: item?.estrusStatus || 'normal',
    focusReason: item?.focusReason || ''
  }
}

export function normalizeDashboardAnimalDetail(item) {
  return {
    animalId: item?.animalId || '-',
    name: item?.name || '-',
    breed: item?.breed || '-',
    deviceId: item?.deviceId || '-',
    age: item?.age ?? null,
    weight: item?.weight ?? null,
    healthScore: round1(item?.healthScore),
    healthStatus: item?.healthStatus || '未知',
    riskLevel: item?.riskLevel || 'normal',
    behaviorStatus: item?.behaviorStatus || 'normal',
    estrusStatus: item?.estrusStatus || 'normal',
    currentTemperature: item?.currentTemperature ?? null,
    currentHeartRate: item?.currentHeartRate ?? null,
    currentActivity: item?.currentActivity ?? null,
    dailyRuminationTime: item?.dailyRuminationTime ?? null,
    dailyFeedingCount: item?.dailyFeedingCount ?? null,
    focusReason: item?.focusReason || '',
    interventionSuggestions: Array.isArray(item?.interventionSuggestions) ? item.interventionSuggestions : [],
    temperatureTrend: Array.isArray(item?.temperatureTrend) ? item.temperatureTrend : [],
    activityTrend: Array.isArray(item?.activityTrend) ? item.activityTrend : [],
    recentRecords: Array.isArray(item?.recentRecords) ? item.recentRecords : []
  }
}

export function normalizeInsight(item) {
  return {
    title: item?.title || '',
    type: item?.type || 'health',
    severity: item?.severity || 'info',
    description: item?.description || '',
    affectedAnimals: Array.isArray(item?.affectedAnimals) ? item.affectedAnimals : [],
    recommendation: item?.recommendation || '',
    confidence: round1(item?.confidence)
  }
}

export function normalizeAlertSummary(raw) {
  const baseline = normalizeBaseline(raw?.baseline)
  return {
    baseline,
    pendingCount: raw?.pendingCount ?? 0,
    acknowledgedCount: raw?.acknowledgedCount ?? 0,
    resolvedCount: raw?.resolvedCount ?? 0,
    totalCount: raw?.totalCount ?? 0,
    urgentCount: raw?.urgentCount ?? 0,
    todayNewCount: raw?.todayNewCount ?? 0,
    todayResolvedCount: raw?.todayResolvedCount ?? 0,
    pushSuccessRate: round1(raw?.pushSuccessRate)
  }
}

export function normalizeAlertEvent(item) {
  return {
    alertId: item?.alertId || '-',
    type: item?.alertType || 'health',
    typeText: item?.alertTypeText || alertTypeTextMap[item?.alertType] || '告警',
    title: item?.title || '暂无标题',
    animalId: item?.animalId || '-',
    animalName: item?.animalName || '-',
    deviceId: item?.deviceId || '-',
    severity: item?.severity || 'medium',
    severityText: item?.severityText || alertSeverityTextMap[item?.severity] || '中风险',
    status: item?.status || 'pending',
    statusText: item?.statusText || alertStatusTextMap[item?.status] || '待处理',
    time: formatDateTime(item?.createdAt),
    createdAt: item?.createdAt || null,
    message: item?.message || '-',
    triggerValue: item?.triggerValue || '-',
    thresholdValue: item?.thresholdValue || '-'
  }
}

export function normalizeAlertDetail(item) {
  return {
    ...normalizeAlertEvent(item),
    animalRiskLevel: item?.animalRiskLevel || 'normal',
    deviceStatus: item?.deviceStatus || '-',
    currentTemperature: item?.currentTemperature ?? null,
    currentHeartRate: item?.currentHeartRate ?? null,
    currentActivity: item?.currentActivity ?? null,
    handlerSuggestion: item?.handlerSuggestion || '',
    acknowledgedAt: item?.acknowledgedAt || null,
    acknowledgedAtText: formatDateTime(item?.acknowledgedAt),
    acknowledgedBy: item?.acknowledgedBy || '-',
    resolvedAt: item?.resolvedAt || null,
    resolvedAtText: formatDateTime(item?.resolvedAt),
    resolvedBy: item?.resolvedBy || '-',
    resolutionNote: item?.resolutionNote || '',
    metricCards: Array.isArray(item?.metricCards) ? item.metricCards : [],
    processRecords: Array.isArray(item?.processRecords) ? item.processRecords : []
  }
}

export function normalizePushHistory(item) {
  return {
    id: item?.id ?? null,
    alertId: item?.alertId || '-',
    channel: item?.channel || '-',
    receiver: item?.receiver || '-',
    content: item?.content || '',
    status: item?.status || '-',
    errorMessage: item?.errorMessage || '',
    pushedAt: item?.pushedAt || null,
    pushedAtText: formatDateTime(item?.pushedAt)
  }
}

export function normalizeAlertRule(item) {
  return {
    id: item?.id ?? null,
    name: item?.name || '',
    type: item?.type || 'health',
    condition: item?.condition || '',
    severity: item?.severity || 'medium',
    enabled: Boolean(item?.enabled),
    pushChannels: Array.isArray(item?.pushChannels) ? item.pushChannels : []
  }
}
