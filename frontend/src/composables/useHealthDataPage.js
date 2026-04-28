import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/api'
import { compactDateTime, metricValue } from '@/utils/formatters'
import { downloadCsv } from '@/utils/exportCsv'
import { useSaveAction } from '@/composables/useSaveAction'

const frequencyOptions = [
  { label: '每分钟', value: 1 },
  { label: '每5分钟', value: 5 },
  { label: '每10分钟', value: 10 },
  { label: '每30分钟', value: 30 },
  { label: '每小时', value: 60 }
]

export function useHealthDataPage() {
  const summary = ref({
    baseline: { totalAnimals: 0, totalDevices: 0 },
    onlineDevices: 0,
    offlineDevices: 0,
    faultDevices: 0,
    devicesWithLowBattery: 0,
    devicesWithWeakSignal: 0,
    abnormalDeviceCount: 0,
    todayDataCompletionRate: 0,
    avgHealthScore: '-',
    avgBatteryLevel: 0
  })
  const devices = ref([])
  const statusFilter = ref('')
  const filterDraft = ref('')
  const { savingAction, runSaveAction } = useSaveAction()

  const showGlobalConfigModal = ref(false)
  const showDeviceDetailModal = ref(false)
  const showDeviceConfigModal = ref(false)
  const showSyncStatusModal = ref(false)
  const showHealthReportModal = ref(false)
  const showFilterModal = ref(false)
  const showReplaceDeviceModal = ref(false)

  const deviceDetailLoading = ref(false)
  const deviceDetail = ref({})
  const deviceLogs = ref([])
  const replaceDeviceTarget = ref({})

  const globalConfig = reactive({
    protocol: 'LoRa',
    uploadInterval: 1,
    temperatureMax: 39.5,
    lowBattery: 20,
    healthModel: '基础模型',
    note: ''
  })

  const deviceConfig = reactive({
    deviceId: '',
    animalId: '',
    protocol: 'LoRa',
    uploadIntervalMinutes: 1,
    temperatureMinThreshold: 38,
    temperatureMaxThreshold: 39.5,
    heartRateMinThreshold: 60,
    heartRateMaxThreshold: 120,
    lowBatteryThreshold: 20,
    installationLocation: '',
    healthModel: '基础模型',
    note: ''
  })

  const syncState = reactive({
    syncing: false,
    lastSyncTime: '-'
  })

  const abnormalDeviceCount = computed(() => Number(summary.value.abnormalDeviceCount || 0))
  const warningCount = computed(() =>
    Number(summary.value.abnormalDataCount || summary.value.alertCount || abnormalDeviceCount.value + Number(summary.value.devicesWithWeakSignal || 0))
  )
  const uploadFrequencyText = computed(() => frequencyLabel(globalConfig.uploadInterval || 1))
  const completionRate = computed(() => clamp(Number(summary.value.todayDataCompletionRate || 96), 0, 100))
  const scoreText = computed(() => metricValue(summary.value.avgHealthScore))
  const healthScoreRate = computed(() => clamp(Number(summary.value.avgHealthScore || 0), 0, 100))
  const avgTemperatureText = computed(() => {
    const temps = devices.value.map((item) => Number(item.currentTemperature)).filter((value) => Number.isFinite(value))
    if (!temps.length) return '-'
    return `${round1(temps.reduce((sum, value) => sum + value, 0) / temps.length)}℃`
  })
  const activityPercentText = computed(() => {
    const values = devices.value.map((item) => Number(item.currentActivity)).filter((value) => Number.isFinite(value))
    if (!values.length) return '-'
    return `${Math.round(values.reduce((sum, value) => sum + value, 0) / values.length)}%`
  })
  const todayCollectText = computed(() => {
    const count = Number(summary.value.todayDataCount || devices.value.length * Math.max(1, Math.round(1440 / Number(globalConfig.uploadInterval || 1))))
    if (count >= 1000000) return `${round1(count / 1000000)}M 条`
    if (count >= 1000) return `${round1(count / 1000)}K 条`
    return `${count} 条`
  })
  const lastUpdateShort = computed(() => {
    const last = devices.value.find((item) => item.lastDataUpdateText && item.lastDataUpdateText !== '-')?.lastDataUpdateText
    return compactTime(last || new Date().toISOString())
  })
  const compactLogs = computed(() => deviceLogs.value.slice(0, 3))
  const installDateText = computed(() => {
    const value = deviceDetail.value.installationDate || deviceDetail.value.createdAt || deviceDetail.value.lastOnlineAt
    return value ? compactDate(value) : '2025-11-15'
  })

  async function loadSummary() {
    summary.value = await api.healthData.getSummary()
  }

  async function loadDevices() {
    devices.value = await api.healthData.getDevices(null, statusFilter.value || null)
  }

  async function loadGlobalConfig() {
    const [deviceMap, thresholdMap, basicMap] = await Promise.all([
      api.system.getConfigMap('device'),
      api.system.getConfigMap('threshold'),
      api.system.getConfigMap('basic')
    ])
    globalConfig.protocol = valueOr(deviceMap.device_protocol, 'LoRa')
    globalConfig.uploadInterval = Number(basicMap.data_upload_interval || 1)
    globalConfig.temperatureMax = Number(thresholdMap.temperature_max || 39.5)
    globalConfig.lowBattery = Number(deviceMap.device_low_battery || 20)
    globalConfig.healthModel = valueOr(deviceMap.health_assessment_model, '基础模型')
    globalConfig.note = valueOr(deviceMap.device_config_note, '')
  }

  function openGlobalConfig() {
    showGlobalConfigModal.value = true
    loadGlobalConfig().catch((error) => api.notify(error.message || '加载设备配置失败', 'danger'))
  }

  function closeGlobalConfig() {
    showGlobalConfigModal.value = false
  }

  async function saveGlobalConfig() {
    await runSaveAction('global-config', async () => {
      try {
        await Promise.all([
          api.system.updateConfig('device_protocol', globalConfig.protocol),
          api.system.updateConfig('data_upload_interval', globalConfig.uploadInterval),
          api.system.updateConfig('temperature_max', globalConfig.temperatureMax),
          api.system.updateConfig('device_low_battery', globalConfig.lowBattery),
          api.system.updateConfig('health_assessment_model', globalConfig.healthModel),
          api.system.updateConfig('device_config_note', globalConfig.note)
        ])
        await Promise.all([loadGlobalConfig(), loadSummary(), loadDevices()])
        api.notify('设备配置已保存')
        closeGlobalConfig()
      } catch (error) {
        api.notify(error.message || '设备配置保存失败', 'danger')
      }
    })
  }

  async function openDeviceDetail(device) {
    deviceDetailLoading.value = true
    showDeviceDetailModal.value = true
    try {
      const [detail, logs] = await Promise.all([
        api.healthData.getDeviceDetail(device.deviceId),
        api.healthData.getDeviceLogs(device.deviceId, 3)
      ])
      deviceDetail.value = detail || {}
      deviceLogs.value = Array.isArray(logs) ? logs : []
    } catch (error) {
      api.notify(error.message || '加载设备详情失败', 'danger')
    } finally {
      deviceDetailLoading.value = false
    }
  }

  function closeDeviceDetail() {
    showDeviceDetailModal.value = false
    deviceDetail.value = {}
    deviceLogs.value = []
  }

  async function openDeviceConfig(device) {
    try {
      const detail = await api.healthData.getDeviceDetail(device.deviceId)
      fillDeviceConfig(detail)
      showDeviceConfigModal.value = true
    } catch (error) {
      api.notify(error.message || '加载设备配置失败', 'danger')
    }
  }

  function openDeviceConfigFromDetail() {
    if (!deviceDetail.value?.deviceId) return
    fillDeviceConfig(deviceDetail.value)
    showDeviceConfigModal.value = true
  }

  function fillDeviceConfig(detail = {}) {
    Object.assign(deviceConfig, {
      deviceId: detail.deviceId || '',
      animalId: detail.animalId || '',
      protocol: detail.protocol || globalConfig.protocol || 'LoRa',
      uploadIntervalMinutes: detail.uploadIntervalMinutes ?? globalConfig.uploadInterval ?? 1,
      temperatureMinThreshold: detail.temperatureMinThreshold ?? 38,
      temperatureMaxThreshold: detail.temperatureMaxThreshold ?? globalConfig.temperatureMax ?? 39.5,
      heartRateMinThreshold: detail.heartRateMinThreshold ?? 60,
      heartRateMaxThreshold: detail.heartRateMaxThreshold ?? 120,
      lowBatteryThreshold: detail.lowBatteryThreshold ?? globalConfig.lowBattery ?? 20,
      installationLocation: detail.installationLocation || '',
      healthModel: detail.healthAssessmentModel || detail.healthModel || globalConfig.healthModel || '基础模型',
      note: detail.note || ''
    })
  }

  function closeDeviceConfig() {
    showDeviceConfigModal.value = false
  }

  async function saveDeviceConfig() {
    await runSaveAction('device-config', async () => {
      try {
        await api.healthData.updateDeviceConfig(deviceConfig.deviceId, {
          animalId: deviceConfig.animalId,
          protocol: deviceConfig.protocol,
          uploadIntervalMinutes: deviceConfig.uploadIntervalMinutes,
          temperatureMinThreshold: deviceConfig.temperatureMinThreshold,
          temperatureMaxThreshold: deviceConfig.temperatureMaxThreshold,
          heartRateMinThreshold: deviceConfig.heartRateMinThreshold,
          heartRateMaxThreshold: deviceConfig.heartRateMaxThreshold,
          lowBatteryThreshold: deviceConfig.lowBatteryThreshold,
          installationLocation: deviceConfig.installationLocation,
          healthAssessmentModel: deviceConfig.healthModel,
          healthModel: deviceConfig.healthModel,
          note: deviceConfig.note
        })
        await Promise.all([loadDevices(), loadSummary()])
        api.notify('设备配置已保存')
        closeDeviceConfig()
      } catch (error) {
        api.notify(error.message || '设备配置保存失败', 'danger')
      }
    })
  }

  function openSyncModal() {
    showSyncStatusModal.value = true
    syncState.lastSyncTime = new Date().toLocaleString('zh-CN')
  }

  async function runSync() {
    syncState.syncing = true
    try {
      await Promise.all([loadSummary(), loadDevices(), loadGlobalConfig()])
      syncState.lastSyncTime = new Date().toLocaleString('zh-CN')
      api.notify('数据同步完成')
    } finally {
      syncState.syncing = false
    }
  }

  function openHealthReport() {
    showHealthReportModal.value = true
  }

  function openFilterModal() {
    filterDraft.value = statusFilter.value
    showFilterModal.value = true
  }

  function closeFilterModal() {
    showFilterModal.value = false
  }

  async function applyFilter() {
    statusFilter.value = filterDraft.value
    await loadDevices()
    closeFilterModal()
  }

  function openReplaceDevice(device) {
    replaceDeviceTarget.value = device
    showReplaceDeviceModal.value = true
  }

  function closeReplaceDevice() {
    showReplaceDeviceModal.value = false
    replaceDeviceTarget.value = {}
  }

  function exportDevicesCsv() {
    const headers = ['设备ID', '牲畜编号', '状态', '最后更新', '体温(℃)', '心率(BPM)', '活动量', '电量']
    const rows = devices.value.map((item) => [
      item.deviceId,
      item.animalId,
      item.statusText,
      item.lastDataUpdateText,
      item.currentTemperature,
      item.currentHeartRate,
      activityLabel(item.currentActivity),
      `${batteryPercent(item.batteryLevel)}%`
    ])
    downloadCsv(`智能项圈设备状态-${new Date().toISOString().slice(0, 10)}.csv`, [headers, ...rows])
  }

  function deviceRate(value) {
    const total = Number(summary.value?.baseline?.totalDevices || 0)
    if (!total) return 0
    return clamp(Math.round((Number(value || 0) / total) * 100), 0, 100)
  }

  onMounted(async () => {
    await Promise.all([loadGlobalConfig(), loadSummary(), loadDevices()])
  })

  return {
    frequencyOptions,
    summary,
    devices,
    statusFilter,
    filterDraft,
    savingAction,
    showGlobalConfigModal,
    showDeviceDetailModal,
    showDeviceConfigModal,
    showSyncStatusModal,
    showHealthReportModal,
    showFilterModal,
    showReplaceDeviceModal,
    deviceDetailLoading,
    deviceDetail,
    deviceLogs,
    replaceDeviceTarget,
    globalConfig,
    deviceConfig,
    syncState,
    abnormalDeviceCount,
    warningCount,
    uploadFrequencyText,
    completionRate,
    scoreText,
    healthScoreRate,
    avgTemperatureText,
    activityPercentText,
    todayCollectText,
    lastUpdateShort,
    compactLogs,
    installDateText,
    openGlobalConfig,
    closeGlobalConfig,
    saveGlobalConfig,
    openDeviceDetail,
    closeDeviceDetail,
    openDeviceConfig,
    openDeviceConfigFromDetail,
    closeDeviceConfig,
    saveDeviceConfig,
    openSyncModal,
    runSync,
    openHealthReport,
    openFilterModal,
    closeFilterModal,
    applyFilter,
    openReplaceDevice,
    closeReplaceDevice,
    exportDevicesCsv,
    deviceRate
  }
}

function frequencyLabel(value) {
  const numeric = Number(value)
  return frequencyOptions.find((item) => item.value === numeric)?.label || `每${numeric}分钟`
}

export function activityLabel(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'string' && /高|中|低/.test(value)) return value
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return String(value)
  if (numeric >= 70) return '高'
  if (numeric >= 35) return '中'
  return '低'
}

export function batteryPercent(value) {
  return clamp(Math.round(Number(value || 0)), 0, 100)
}

export function batteryTone(value) {
  const percent = batteryPercent(value)
  if (percent <= 20) return 'manual32-battery-low'
  if (percent <= 50) return 'manual32-battery-mid'
  return 'manual32-battery-high'
}

export function signalText(value) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return '-'
  if (numeric <= 0) return `${numeric} dBm`
  return `-${Math.round(100 - numeric)} dBm`
}

export function compactDate(value) {
  const date = parseDate(value)
  if (!date) return value || '-'
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function compactTime(value) {
  if (!value || value === '-') return '-'
  const date = parseDate(value)
  if (!date) {
    const matched = String(value).match(/\d{1,2}:\d{2}/)
    return matched?.[0] || value
  }
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function parseDate(value) {
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  const direct = new Date(value)
  if (!Number.isNaN(direct.getTime())) return direct
  const normalized = String(value).replace(/-/g, '/')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function valueOr(value, fallback) {
  return value === null || value === undefined || value === '' ? fallback : value
}

function round1(value) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return 0
  return Math.round(numeric * 10) / 10
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function pad(value) {
  return String(value).padStart(2, '0')
}
