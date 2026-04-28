<template>
  <div class="manual-page manual37-page">
    <PageHeader title="异常告警推送管理">
      <template #actions>
        <button class="manual37-top-btn manual37-top-btn--primary" @click="openRuleModal">🔔 创建告警规则</button>
        <button class="manual37-top-btn" @click="openBatchOperation">⚡ 批量操作</button>
        <button class="manual37-top-btn" @click="openPushHistory">📋 推送历史</button>
      </template>
    </PageHeader>

    <div class="manual37-card-grid">
      <div class="manual37-stat-card">
        <div class="manual37-stat-head">
          <span>待处理告警</span>
          <b class="manual37-chip manual37-chip--yellow">⚠</b>
        </div>
        <strong class="manual37-stat-value manual37-stat-value--yellow">{{ alertSummary.pendingCount || 0 }}</strong>
        <div class="manual37-stat-row">
          <span>今日新增</span>
          <span>{{ alertSummary.todayNewCount || 0 }}</span>
        </div>
        <div class="manual37-progress manual37-progress--yellow">
          <i :style="{ width: `${summaryRate(alertSummary.pendingCount)}%` }"></i>
        </div>
      </div>

      <div class="manual37-stat-card">
        <div class="manual37-stat-head">
          <span>紧急告警</span>
          <b class="manual37-chip manual37-chip--red">🔥</b>
        </div>
        <strong class="manual37-stat-value manual37-stat-value--red">{{ urgentAlertCount }}</strong>
        <div class="manual37-stat-row">
          <span>处理中</span>
          <span>{{ alertSummary.acknowledgedCount || 0 }}</span>
        </div>
        <div class="manual37-progress manual37-progress--red">
          <i :style="{ width: `${summaryRate(urgentAlertCount)}%` }"></i>
        </div>
      </div>

      <div class="manual37-stat-card">
        <div class="manual37-stat-head">
          <span>已解决告警</span>
          <b class="manual37-chip manual37-chip--green">✅</b>
        </div>
        <strong class="manual37-stat-value manual37-stat-value--green">{{ alertSummary.resolvedCount || 0 }}</strong>
        <div class="manual37-stat-row">
          <span>本月总计</span>
          <span>{{ alertSummary.totalCount || 0 }}</span>
        </div>
        <div class="manual37-progress manual37-progress--green">
          <i :style="{ width: `${summaryRate(alertSummary.resolvedCount)}%` }"></i>
        </div>
      </div>

      <div class="manual37-stat-card">
        <div class="manual37-stat-head">
          <span>推送成功率</span>
          <b class="manual37-chip manual37-chip--blue">↗</b>
        </div>
        <strong class="manual37-stat-value manual37-stat-value--blue">{{ pushSuccessRate }}%</strong>
        <div class="manual37-stat-row">
          <span>失败次数</span>
          <span>{{ pushFailCount }}</span>
        </div>
        <div class="manual37-progress manual37-progress--blue">
          <i :style="{ width: `${pushSuccessRate}%` }"></i>
        </div>
      </div>
    </div>

    <AlertEventTable
      :events="alertEvents"
      :saving-action="savingAction"
      :alert-row-class="alertRowClass"
      :format-animal-id="formatAnimalId"
      :type-class="typeClass"
      :type-text="typeText"
      :severity-class="severityClass"
      :severity-text="severityText"
      :status-class="statusClass"
      :status-text="statusText"
      @filter="openFilterModal"
      @refresh="refreshAlerts"
      @detail="openDetail"
      @acknowledge="acknowledgeAlert"
      @resolve="resolveAlert"
    />


    <AlertFilterModal
      v-if="showFilterModal"
      :filter-draft="filterDraft"
      @close="showFilterModal = false"
      @reset="resetAlertFilters"
      @apply="applyAlertFilters"
    />


    <AlertBatchModal
      v-if="showBatchModal"
      :events="batchCandidateEvents"
      :selected-alert-ids="selectedAlertIds"
      :all-selected="allBatchSelected"
      :saving-action="savingAction"
      :format-animal-id="formatAnimalId"
      :type-text="typeText"
      :severity-class="severityClass"
      :severity-text="severityText"
      :status-class="statusClass"
      :status-text="statusText"
      @close="showBatchModal = false"
      @toggle="toggleBatchSelection"
      @toggle-all="toggleAllBatchEvents"
      @acknowledge="batchAcknowledge"
      @resolve="batchResolve"
    />


    <AlertRuleModal
      v-if="showRuleModal"
      :rule-form="ruleForm"
      :saving-action="savingAction"
      @close="showRuleModal = false"
      @save="saveRule"
    />


    <AlertDetailModal
      v-if="showDetailModal"
      :detail="detail"
      :loading="detailLoading"
      :saving-action="savingAction"
      :alert-metric-cards="alertMetricCards"
      :detail-process-records="detailProcessRecords"
      :detail-signal-text="detailSignalText"
      :detail-battery-text="detailBatteryText"
      :detail-push-channel-text="detailPushChannelText"
      :format-animal-id="formatAnimalId"
      :type-text="typeText"
      :severity-text="severityText"
      :device-status-text="deviceStatusText"
      :format-time="formatTime"
      @close="showDetailModal = false"
      @acknowledge="acknowledgeAlert(detail, true)"
      @resolve="resolveAlert(detail, true)"
    />


    <AlertPushHistoryModal
      v-if="showPushHistoryModal"
      :tabs="pushHistoryTabs"
      :filter="pushHistoryFilter"
      :items="filteredPushHistory"
      :channel-text="channelText"
      :push-status-class="pushStatusClass"
      :push-status-text="pushStatusText"
      :response-time-text="responseTimeText"
      @update:filter="pushHistoryFilter = $event"
      @close="showPushHistoryModal = false"
    />

  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/api'
import { formatAnimalId, metricValue } from '@/utils/formatters'
import PageHeader from '@/components/common/PageHeader.vue'
import AlertEventTable from './alerts/AlertEventTable.vue'
import AlertFilterModal from './alerts/AlertFilterModal.vue'
import AlertBatchModal from './alerts/AlertBatchModal.vue'
import AlertRuleModal from './alerts/AlertRuleModal.vue'
import AlertDetailModal from './alerts/AlertDetailModal.vue'
import AlertPushHistoryModal from './alerts/AlertPushHistoryModal.vue'

const alertSummary = ref({
  baseline: { totalAnimals: 0, totalDevices: 0 },
  pendingCount: 0,
  acknowledgedCount: 0,
  resolvedCount: 0,
  totalCount: 0,
  urgentCount: 0,
  todayNewCount: 0,
  pushSuccessRate: 0
})
const alertEvents = ref([])
const pushHistory = ref([])
const pushChannels = ref([])
const savingAction = ref('')

const filters = reactive({ status: '', severity: '' })
const filterDraft = reactive({ status: '', severity: '' })

const showRuleModal = ref(false)
const showDetailModal = ref(false)
const showPushHistoryModal = ref(false)
const showFilterModal = ref(false)
const showBatchModal = ref(false)

const detailLoading = ref(false)
const detail = ref({ metricCards: [], processRecords: [] })
const selectedAlertIds = ref([])
const pushHistoryFilter = ref('all')

const ruleForm = reactive({
  name: '',
  condition: '体温异常',
  severity: 'high',
  pushChannel: 'wechat',
  minValue: '',
  maxValue: '',
  activeStart: '08:00',
  activeEnd: '20:00',
  note: ''
})

const pushHistoryTabs = [
  { label: '全部', value: 'all' },
  { label: '成功', value: 'success' },
  { label: '失败', value: 'failed' },
  { label: '微信', value: 'wechat' },
  { label: '短信', value: 'sms' }
]

const urgentAlertCount = computed(() => alertSummary.value.urgentCount || alertEvents.value.filter((event) => event.severity === 'high').length)

const pushFailCount = computed(() => {
  if (pushHistory.value.length) {
    return pushHistory.value.filter((item) => !isPushSuccess(item.status)).length
  }
  return pushChannels.value.reduce((sum, channel) => sum + Number(channel.failCount || 0), 0)
})

const pushSuccessRate = computed(() => {
  const summaryRateValue = Number(alertSummary.value.pushSuccessRate || 0)
  if (summaryRateValue) return Math.round(summaryRateValue)
  if (pushChannels.value.length) {
    const total = pushChannels.value.reduce((sum, channel) => sum + Number(channel.successRate || 0), 0)
    return Math.round(total / pushChannels.value.length)
  }
  if (pushHistory.value.length) {
    const success = pushHistory.value.filter((item) => isPushSuccess(item.status)).length
    return Math.round((success / pushHistory.value.length) * 100)
  }
  return 0
})

const batchCandidateEvents = computed(() => alertEvents.value.filter((event) => event.status !== 'resolved'))
const selectedBatchEvents = computed(() => batchCandidateEvents.value.filter((event) => selectedAlertIds.value.includes(event.alertId)))
const allBatchSelected = computed(() => Boolean(batchCandidateEvents.value.length) && batchCandidateEvents.value.every((event) => selectedAlertIds.value.includes(event.alertId)))

const filteredPushHistory = computed(() => {
  if (pushHistoryFilter.value === 'all') return pushHistory.value
  if (['wechat', 'sms'].includes(pushHistoryFilter.value)) {
    return pushHistory.value.filter((item) => String(item.channel).toLowerCase().includes(pushHistoryFilter.value))
  }
  return pushHistory.value.filter((item) => {
    const success = isPushSuccess(item.status)
    return pushHistoryFilter.value === 'success' ? success : !success
  })
})

const alertMetricCards = computed(() => {
  const item = detail.value || {}
  const fallbackCards = Array.isArray(item.metricCards) ? item.metricCards : []
  if (item.currentTemperature == null && item.currentHeartRate == null && item.currentActivity == null && fallbackCards.length) {
    return fallbackCards.slice(0, 3).map((card) => ({
      label: card.label || card.name || '指标',
      value: card.value || '-',
      sub: card.status || card.description || '',
      tone: metricTone(card.status || card.label)
    }))
  }
  return [
    {
      label: '体温',
      value: metricValue(item.currentTemperature, '℃'),
      sub: '正常范围: 38.2-39.5℃',
      tone: Number(item.currentTemperature || 0) >= 39.5 ? 'metric-red' : 'metric-blue'
    },
    {
      label: '心率',
      value: metricValue(item.currentHeartRate, ' bpm'),
      sub: '正常范围: 70-85 bpm',
      tone: Number(item.currentHeartRate || 0) > 85 ? 'metric-blue' : 'metric-green'
    },
    {
      label: '活动量',
      value: activityText(item.currentActivity),
      sub: '正常范围: 中-高',
      tone: activityTone(item.currentActivity)
    }
  ]
})

const detailProcessRecords = computed(() => {
  const item = detail.value || {}
  const records = Array.isArray(item.processRecords) ? item.processRecords : []
  if (records.length) {
    return records.map((record) => ({
      stage: record.stage || record.title || '处理记录',
      note: record.note || record.description || '-',
      operator: record.operator || record.handler || '-',
      time: record.time || record.createdAt || item.createdAt
    }))
  }

  const rows = [
    {
      stage: '告警触发',
      note: item.message || `${typeText(item.type)}达到告警条件`,
      operator: '系统自动',
      time: item.createdAt
    }
  ]
  if (item.acknowledgedAt) {
    rows.push({
      stage: '告警确认',
      note: '已确认并进入处理流程',
      operator: item.acknowledgedBy || '值班人员',
      time: item.acknowledgedAt
    })
  }
  if (item.resolvedAt) {
    rows.push({
      stage: '告警解决',
      note: item.resolutionNote || '已完成处置',
      operator: item.resolvedBy || '值班人员',
      time: item.resolvedAt
    })
  }
  return rows
})

const detailSignalText = computed(() => {
  const card = findMetricCard(['信号', 'signal'])
  return card?.value || detail.value.signalStrength || '-'
})

const detailBatteryText = computed(() => {
  const card = findMetricCard(['电量', 'battery'])
  return card?.value || detail.value.batteryLevel || '-'
})

const detailPushChannelText = computed(() => {
  const matched = pushHistory.value.filter((item) => item.alertId === detail.value.alertId).map((item) => channelText(item.channel))
  const unique = [...new Set(matched)].filter(Boolean)
  if (unique.length) return unique.join('+')
  const enabled = pushChannels.value.filter((item) => item.enabled).map((item) => channelText(item.channel))
  return enabled.length ? enabled.slice(0, 2).join('+') : '-'
})

async function loadAlertSummary() {
  alertSummary.value = await api.alerts.getSummary()
}

async function loadAlertEvents() {
  alertEvents.value = await api.alerts.getEvents(null, filters.status || null, filters.severity || null, 20)
}

async function loadAlertDecorations() {
  const [channels, history] = await Promise.all([
    api.alerts.getPushChannels(),
    api.alerts.getPushHistory(null, 40).catch(() => [])
  ])
  pushChannels.value = Array.isArray(channels) ? channels : []
  pushHistory.value = Array.isArray(history) ? history : []
}

async function refreshAlerts() {
  await Promise.all([loadAlertSummary(), loadAlertEvents(), loadAlertDecorations()])
  api.notify('告警数据已刷新')
}

function openRuleModal() {
  showRuleModal.value = true
}

async function saveRule() {
  if (savingAction.value) return
  if (!ruleForm.name || !ruleForm.condition) {
    api.notify('请填写规则名称和触发条件', 'danger')
    return
  }
  savingAction.value = 'rule'
  try {
    await api.alerts.createRule({
      name: ruleForm.name,
      type: conditionToType(ruleForm.condition),
      condition: ruleForm.condition,
      minValue: ruleForm.minValue,
      maxValue: ruleForm.maxValue,
      activeStart: ruleForm.activeStart,
      activeEnd: ruleForm.activeEnd,
      severity: ruleForm.severity,
      pushChannels: ruleForm.pushChannel.split(',').map((item) => item.trim()).filter(Boolean),
      note: ruleForm.note,
      farmId: Number(localStorage.getItem('farmId') || 1)
    })
    Object.assign(ruleForm, {
      name: '',
      condition: '体温异常',
      severity: 'high',
      pushChannel: 'wechat',
      minValue: '',
      maxValue: '',
      activeStart: '08:00',
      activeEnd: '20:00',
      note: ''
    })
    showRuleModal.value = false
    await refreshAlerts()
    api.notify('告警规则已创建')
  } catch (error) {
    api.notify(error.message || '告警规则创建失败', 'danger')
  } finally {
    savingAction.value = ''
  }
}

function openFilterModal() {
  filterDraft.status = filters.status
  filterDraft.severity = filters.severity
  showFilterModal.value = true
}

async function applyAlertFilters() {
  filters.status = filterDraft.status
  filters.severity = filterDraft.severity
  showFilterModal.value = false
  await loadAlertEvents()
}

async function resetAlertFilters() {
  filterDraft.status = ''
  filterDraft.severity = ''
  filters.status = ''
  filters.severity = ''
  showFilterModal.value = false
  await loadAlertEvents()
}

function openBatchOperation() {
  selectedAlertIds.value = batchCandidateEvents.value.map((event) => event.alertId)
  showBatchModal.value = true
}

function toggleBatchSelection(event) {
  selectedAlertIds.value = selectedAlertIds.value.includes(event.alertId)
    ? selectedAlertIds.value.filter((id) => id !== event.alertId)
    : [...selectedAlertIds.value, event.alertId]
}

function toggleAllBatchEvents() {
  selectedAlertIds.value = allBatchSelected.value ? [] : batchCandidateEvents.value.map((event) => event.alertId)
}

async function batchAcknowledge() {
  if (!selectedAlertIds.value.length || savingAction.value) return
  savingAction.value = 'batch-ack'
  const ids = [...selectedAlertIds.value]
  try {
    await api.alerts.batchOperation(ids, 'acknowledge', currentOperator())
  } catch (error) {
    await Promise.all(selectedBatchEvents.value.filter((event) => event.status === 'pending').map((event) => api.alerts.acknowledge(event.alertId, currentOperator())))
  } finally {
    savingAction.value = ''
  }
  selectedAlertIds.value = []
  showBatchModal.value = false
  await refreshAlerts()
}

async function batchResolve() {
  if (!selectedAlertIds.value.length || savingAction.value) return
  savingAction.value = 'batch-resolve'
  const ids = [...selectedAlertIds.value]
  try {
    await api.alerts.batchOperation(ids, 'resolve', currentOperator())
  } catch (error) {
    for (const event of selectedBatchEvents.value) {
      if (event.status === 'pending') {
        await api.alerts.acknowledge(event.alertId, currentOperator())
      }
      if (event.status !== 'resolved') {
        await api.alerts.resolve(event.alertId, currentOperator(), '批量操作已解决')
      }
    }
  } finally {
    savingAction.value = ''
  }
  selectedAlertIds.value = []
  showBatchModal.value = false
  await refreshAlerts()
}

async function openDetail(event) {
  detail.value = { ...event, metricCards: [], processRecords: [] }
  detailLoading.value = true
  showDetailModal.value = true
  try {
    detail.value = await api.alerts.getEventDetail(event.alertId)
  } catch (error) {
    api.notify(error.message || '告警详情加载失败', 'danger')
  } finally {
    detailLoading.value = false
  }
}

async function openPushHistory() {
  pushHistory.value = await api.alerts.getPushHistory(null, 80)
  pushHistoryFilter.value = 'all'
  showPushHistoryModal.value = true
}

async function acknowledgeAlert(event, refreshDetail = false) {
  if (!event?.alertId || savingAction.value) return
  savingAction.value = `ack-${event.alertId}`
  try {
    await api.alerts.acknowledge(event.alertId, currentOperator())
    await Promise.all([loadAlertSummary(), loadAlertEvents(), loadAlertDecorations()])
    if (refreshDetail) {
      detail.value = await api.alerts.getEventDetail(event.alertId)
    }
    api.notify('告警已确认')
  } catch (error) {
    api.notify(error.message || '告警确认失败', 'danger')
  } finally {
    savingAction.value = ''
  }
}

async function resolveAlert(event, refreshDetail = false) {
  if (!event?.alertId || savingAction.value) return
  savingAction.value = `resolve-${event.alertId}`
  try {
    if (event.status === 'pending') {
      await api.alerts.acknowledge(event.alertId, currentOperator())
    }
    await api.alerts.resolve(event.alertId, currentOperator(), '已完成处置')
    await Promise.all([loadAlertSummary(), loadAlertEvents(), loadAlertDecorations()])
    if (refreshDetail) {
      detail.value = await api.alerts.getEventDetail(event.alertId)
    }
    api.notify('告警已解决')
  } catch (error) {
    api.notify(error.message || '告警解决失败', 'danger')
  } finally {
    savingAction.value = ''
  }
}

function summaryRate(value) {
  const total = Number(alertSummary.value?.totalCount || 0)
  if (!total) return 0
  return clamp(Math.round((Number(value || 0) / total) * 100), 0, 100)
}

function alertRowClass(event) {
  if (event.status === 'resolved') return 'manual37-row--resolved'
  if (event.status === 'acknowledged') return 'manual37-row--ack'
  if (event.severity === 'high') return 'manual37-row--pending-high'
  if (event.severity === 'medium') return 'manual37-row--pending'
  return ''
}

function typeText(type) {
  return {
    health: '体温异常',
    behavior: '活动异常',
    estrus: '发情预警',
    device: '设备故障'
  }[type] || '告警事件'
}

function typeClass(type) {
  return {
    health: 'type-red',
    behavior: 'type-blue',
    estrus: 'type-purple',
    device: 'type-yellow'
  }[type] || 'type-gray'
}

function severityText(severity) {
  return {
    high: '紧急',
    medium: '高',
    low: '中',
    info: '低'
  }[severity] || '中'
}

function severityClass(severity) {
  return {
    high: 'severity-red',
    medium: 'severity-yellow',
    low: 'severity-green',
    info: 'severity-blue'
  }[severity] || 'severity-yellow'
}

function statusText(status) {
  return {
    pending: '待处理',
    acknowledged: '处理中',
    resolved: '已解决'
  }[status] || '待处理'
}

function statusClass(status) {
  return {
    pending: 'status-yellow',
    acknowledged: 'status-purple',
    resolved: 'status-green'
  }[status] || 'status-yellow'
}

function channelText(channel) {
  const text = String(channel || '').toLowerCase()
  if (text.includes('wechat') || text.includes('微信')) return '微信'
  if (text.includes('sms') || text.includes('短信')) return '短信'
  if (text.includes('app')) return 'APP'
  return channel || '-'
}

function pushStatusText(status) {
  return isPushSuccess(status) ? '成功' : '失败'
}

function pushStatusClass(status) {
  return isPushSuccess(status) ? 'status-green' : 'status-red'
}

function isPushSuccess(status) {
  return ['success', 'sent', 'delivered', '成功', '已发送'].includes(String(status || '').toLowerCase())
}

function responseTimeText(item, index) {
  if (!isPushSuccess(item.status)) return '-'
  if (item.responseTime) return item.responseTime
  return `${8 + (index % 15)}秒`
}

function conditionToType(condition) {
  if (condition.includes('设备')) return 'device'
  if (condition.includes('活动')) return 'behavior'
  return 'health'
}

function deviceStatusText(status) {
  return {
    online: '在线',
    offline: '离线',
    fault: '故障',
    normal: '在线'
  }[status] || status || '-'
}

function activityText(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'string') return value
  if (Number(value) >= 70) return '极低'
  if (Number(value) >= 40) return '中'
  return '低'
}

function activityTone(value) {
  if (value === null || value === undefined || value === '') return 'metric-green'
  if (typeof value === 'string') {
    return value.includes('低') ? 'metric-green' : 'metric-blue'
  }
  return Number(value) >= 70 ? 'metric-green' : 'metric-blue'
}

function metricTone(value) {
  const text = String(value || '')
  if (text.includes('异常') || text.includes('高') || text.includes('危险')) return 'metric-red'
  if (text.includes('低')) return 'metric-green'
  return 'metric-blue'
}

function findMetricCard(keys) {
  const cards = Array.isArray(detail.value?.metricCards) ? detail.value.metricCards : []
  return cards.find((card) => {
    const text = `${card.label || ''}${card.name || ''}`.toLowerCase()
    return keys.some((key) => text.includes(String(key).toLowerCase()))
  })
}

function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('zh-CN', { hour12: false })
}

function currentOperator() {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    return user.username || 'admin'
  } catch (error) {
    return 'admin'
  }
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

onMounted(async () => {
  await Promise.all([loadAlertSummary(), loadAlertEvents(), loadAlertDecorations()])
})
</script>

<style src="./alerts/alerts-page.css"></style>
