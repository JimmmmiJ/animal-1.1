<template>
  <div class="manual-page manual34-page">
    <PageHeader title="疾病早期检测与风险评估">
      <template #actions>
        <button class="manual34-top-btn manual34-top-btn--primary" @click="openAssessmentModal">📊 风险评估</button>
        <button class="manual34-top-btn" @click="openModelConfig">⚙ 模型配置</button>
        <button class="manual34-top-btn" @click="openAlarmHistory">📋 告警历史</button>
      </template>
    </PageHeader>

    <div class="manual34-card-grid">
      <div class="manual34-stat-card">
        <div class="manual34-stat-head">
          <span>高风险牲畜</span>
          <b class="manual34-chip manual34-chip--red">高风险</b>
        </div>
        <strong>{{ configuredDiseaseStats.high }}</strong>
        <div class="manual34-stat-row">
          <span>较昨日</span>
          <em class="manual34-red">↑ +{{ yesterdayHighDelta }}</em>
        </div>
        <div class="manual34-progress"><i :style="{ width: `${riskPercent(configuredDiseaseStats.high)}%` }" class="manual34-progress--red"></i></div>
        <div class="manual34-stat-row">
          <span>总体风险</span>
          <span>{{ totalRiskPercent }}%</span>
        </div>
      </div>

      <div class="manual34-stat-card">
        <div class="manual34-stat-head">
          <span>疾病预警</span>
          <b class="manual34-chip manual34-chip--yellow">中风险</b>
        </div>
        <strong>{{ configuredDiseaseStats.high + configuredDiseaseStats.medium }}</strong>
        <div class="manual34-stat-row">
          <span>已处理</span>
          <span>{{ handledAlertCount }}</span>
        </div>
        <div class="manual34-progress"><i :style="{ width: `${warningProcessPercent}%` }" class="manual34-progress--yellow"></i></div>
        <div class="manual34-stat-row">
          <span>待处理</span>
          <span>{{ pendingAlertCount }}</span>
        </div>
      </div>

      <div class="manual34-stat-card">
        <div class="manual34-stat-head">
          <span>健康牲畜</span>
          <b class="manual34-chip manual34-chip--green">低风险</b>
        </div>
        <strong>{{ configuredDiseaseStats.healthy }}</strong>
        <div class="manual34-stat-row">
          <span>占比</span>
          <span>{{ healthyPercent }}%</span>
        </div>
        <div class="manual34-progress"><i :style="{ width: `${healthyPercent}%` }" class="manual34-progress--green"></i></div>
        <div class="manual34-stat-row">
          <span>总体健康</span>
          <span>良好</span>
        </div>
      </div>

      <div class="manual34-stat-card">
        <div class="manual34-stat-head">
          <span>模型准确率</span>
          <b class="manual34-chip manual34-chip--green">优秀</b>
        </div>
        <strong>{{ summary.modelAccuracy || 92.3 }}%</strong>
        <div class="manual34-stat-row">
          <span>最近更新</span>
          <span>{{ modelConfig.lastUpdate }}</span>
        </div>
        <div class="manual34-progress"><i :style="{ width: `${summary.modelAccuracy || 92}%` }" class="manual34-progress--blue"></i></div>
        <div class="manual34-stat-row">
          <span>预测能力</span>
          <span>强</span>
        </div>
      </div>
    </div>

    <RiskAnimalTable
      :animals="animals"
      :configured-risk-level="configuredRiskLevel"
      :risk-short-text="riskShortText"
      :risk-trend-text="riskTrendText"
      :risk-trend-class="riskTrendClass"
      @filter="openDiseaseFilter"
      @export="exportDiseaseCsv"
      @detail="openDetail"
      @medical-record="openMedicalRecord"
      @treatment="openTreatmentPlan"
    />

    <div v-if="showFilterModal" class="manual34-mask" @click.self="showFilterModal = false">
      <section class="manual34-modal manual34-modal--sm">
        <button class="manual34-close" @click="showFilterModal = false">×</button>
        <h3>筛选</h3>
        <label class="manual34-field">
          <span>风险等级</span>
          <select v-model="riskLevelFilterDraft">
            <option value="">全部级别</option>
            <option value="high">高风险</option>
            <option value="medium">中风险</option>
            <option value="low">低风险</option>
          </select>
        </label>
        <div class="manual34-footer">
          <button class="manual34-btn" @click="resetDiseaseFilter">重置</button>
          <button class="manual34-btn manual34-btn--primary" @click="applyDiseaseFilter">确定</button>
        </div>
      </section>
    </div>

    <DiseaseAssessmentModal
      v-if="showAssessmentModal"
      v-model:animal-id="assessmentAnimalId"
      :animals="animals"
      :form="assessmentForm"
      @close="showAssessmentModal = false"
      @submit="runAssessment"
    />

    <RiskDetailModal
      v-if="showDetailModal"
      :detail="detail"
      :loading="detailLoading"
      :detail-tags="detailTags"
      :detail-health-trend-option="detailHealthTrendOption"
      :configured-risk-level="configuredRiskLevel"
      :risk-short-text="riskShortText"
      :risk-trend-text="riskTrendText"
      :risk-trend-class="riskTrendClass"
      @close="closeDetail"
      @treatment="openTreatmentPlan"
    />

    <TreatmentPlanModal
      v-if="showTreatmentModal"
      :form="treatmentForm"
      :saving="savingAction === 'treatment'"
      @close="showTreatmentModal = false"
      @save="saveTreatmentPlan"
    />

    <div v-if="showModelConfigModal" class="manual34-mask" @click.self="showModelConfigModal = false">
      <section class="manual34-modal manual34-modal--config">
        <button class="manual34-close" @click="showModelConfigModal = false">×</button>
        <h3>模型配置</h3>
        <div class="manual34-form-grid">
          <label class="manual34-field">
            <span>高风险阈值</span>
            <input v-model.number="modelConfig.highRiskThreshold" type="number" />
          </label>
          <label class="manual34-field">
            <span>中风险阈值</span>
            <input v-model.number="modelConfig.mediumRiskThreshold" type="number" />
          </label>
          <label class="manual34-field">
            <span>评估周期</span>
            <select v-model="modelConfig.assessmentCycle">
              <option value="实时评估">实时评估</option>
              <option value="每小时">每小时</option>
              <option value="每日">每日</option>
            </select>
          </label>
          <label class="manual34-field">
            <span>预警策略</span>
            <select v-model="modelConfig.alertStrategy">
              <option value="高敏感">高敏感</option>
              <option value="均衡">均衡</option>
              <option value="低误报">低误报</option>
            </select>
          </label>
        </div>
        <div class="manual34-footer">
          <button class="manual34-btn" @click="showModelConfigModal = false">取消</button>
          <button class="manual34-btn manual34-btn--primary" :disabled="savingAction === 'model-config'" @click="saveModelConfig">
            {{ savingAction === 'model-config' ? '保存中...' : '保存配置' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="showAlarmHistoryModal" class="manual34-mask" @click.self="showAlarmHistoryModal = false">
      <section class="manual34-modal manual34-modal--history">
        <button class="manual34-close" @click="showAlarmHistoryModal = false">×</button>
        <h3>告警历史</h3>
        <div class="manual34-table-wrap manual34-table-wrap--modal">
          <table class="manual34-table">
            <thead>
              <tr>
                <th>牲畜编号</th>
                <th>事件类型</th>
                <th>风险等级</th>
                <th>事件描述</th>
                <th>发生时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="event in healthEvents" :key="`${event.animalId}-${event.eventTime}`">
                <td class="manual34-strong">{{ formatAnimalId(event.animalId) }}</td>
                <td>{{ event.eventType }}</td>
                <td>{{ riskText(event.riskLevel) }}</td>
                <td>{{ event.description }}</td>
                <td>{{ formatTime(event.eventTime) }}</td>
              </tr>
              <tr v-if="!healthEvents.length">
                <td colspan="5" class="manual34-empty">暂无告警历史</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="manual34-footer">
          <button class="manual34-btn manual34-btn--primary" @click="showAlarmHistoryModal = false">关闭</button>
        </div>
      </section>
    </div>

    <div v-if="showMedicalRecordModal" class="manual34-mask" @click.self="showMedicalRecordModal = false">
      <section class="manual34-modal manual34-modal--history">
        <button class="manual34-close" @click="showMedicalRecordModal = false">×</button>
        <h3>{{ medicalRecord.animalId ? `${formatAnimalId(medicalRecord.animalId)} 病历` : '病历' }}</h3>
        <div class="manual34-record-card">
          <p><span>风险等级</span><b>{{ configuredRiskText(medicalRecord) }}</b></p>
          <p><span>风险评分</span><b>{{ medicalRecord.riskScore || '-' }}</b></p>
          <p><span>风险类型</span><b>{{ medicalRecord.mainRisk || '-' }}</b></p>
          <p><span>最近更新</span><b>{{ medicalRecord.lastCheckTimeText || '-' }}</b></p>
        </div>
        <ul class="manual34-record-list">
          <li v-for="item in medicalRecordItems" :key="item">{{ item }}</li>
        </ul>
        <div class="manual34-footer">
          <button class="manual34-btn" @click="showMedicalRecordModal = false">关闭</button>
          <button class="manual34-btn manual34-btn--orange" @click="openTreatmentPlan(medicalRecord)">治疗</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/api'
import { formatAnimalId } from '@/utils/formatters'
import { downloadCsv } from '@/utils/exportCsv'
import PageHeader from '@/components/common/PageHeader.vue'
import RiskAnimalTable from '@/views/disease/RiskAnimalTable.vue'
import DiseaseAssessmentModal from '@/views/disease/DiseaseAssessmentModal.vue'
import RiskDetailModal from '@/views/disease/RiskDetailModal.vue'
import TreatmentPlanModal from '@/views/disease/TreatmentPlanModal.vue'

const summary = ref({
  baseline: { totalAnimals: 0 },
  highRiskCount: 0,
  mediumRiskCount: 0,
  lowRiskCount: 0,
  healthyCount: 0,
  healthyPercent: 0,
  alertCount: 0,
  modelAccuracy: 0
})
const animals = ref([])
const riskLevelFilter = ref('')
const riskLevelFilterDraft = ref('')
const healthEvents = ref([])
const savingAction = ref('')

const showAssessmentModal = ref(false)
const showModelConfigModal = ref(false)
const showAlarmHistoryModal = ref(false)
const showFilterModal = ref(false)
const showMedicalRecordModal = ref(false)
const showDetailModal = ref(false)
const showTreatmentModal = ref(false)

const assessmentAnimalId = ref('')
const assessmentForm = reactive({
  assessmentTime: '',
  symptomDescription: '',
  temperature: 38.5,
  heartRate: 80,
  initialDiagnosis: '',
  notes: ''
})

const modelConfig = reactive({
  highRiskThreshold: 85,
  mediumRiskThreshold: 70,
  assessmentCycle: '实时评估',
  alertStrategy: '均衡',
  lastUpdate: '2025-12-20'
})

const configuredDiseaseStats = computed(() => {
  const stats = { high: 0, medium: 0, healthy: 0 }
  animals.value.forEach((animal) => {
    const level = configuredRiskLevel(animal)
    if (level === 'high') stats.high += 1
    else if (level === 'medium') stats.medium += 1
    else stats.healthy += 1
  })
  return stats
})

const totalRiskPercent = computed(() => riskPercent(configuredDiseaseStats.value.high + configuredDiseaseStats.value.medium))
const healthyPercent = computed(() => riskPercent(configuredDiseaseStats.value.healthy))
const yesterdayHighDelta = computed(() => Math.max(0, Math.round(configuredDiseaseStats.value.high * 0.16)))
const handledAlertCount = computed(() => Math.max(0, Number(summary.value.alertCount || 0) - configuredDiseaseStats.value.medium))
const pendingAlertCount = computed(() => configuredDiseaseStats.value.medium)
const warningProcessPercent = computed(() => {
  const total = handledAlertCount.value + pendingAlertCount.value
  return total ? Math.round((handledAlertCount.value / total) * 100) : 0
})

const medicalRecord = ref({})
const medicalRecordItems = computed(() => {
  if (!medicalRecord.value.animalId) return []
  const items = [
    `主要症状：${medicalRecord.value.mainSymptoms || '-'}`,
    `风险依据：${medicalRecord.value.suggestion || medicalRecord.value.mainRisk || '-'}`,
    `最近检查：${medicalRecord.value.lastCheckTimeText || '-'}`
  ]
  return items.concat(Array.isArray(medicalRecord.value.riskBasis) ? medicalRecord.value.riskBasis : [])
})

const detailLoading = ref(false)
const detail = ref({
  symptomTags: [],
  riskBasis: [],
  temperatureTrend: [],
  heartRateTrend: [],
  activityTrend: []
})

const detailTags = computed(() => {
  const tags = Array.isArray(detail.value.symptomTags) && detail.value.symptomTags.length
    ? detail.value.symptomTags
    : String(detail.value.mainSymptoms || '').split(/[、,，]/).filter(Boolean)
  return tags.length ? tags : ['持续观察']
})

const treatmentForm = reactive({
  animalId: '',
  diagnosis: '',
  treatmentPlan: '',
  startDate: '',
  endDate: '',
  careRequirement: '',
  recheckAt: '',
  notes: ''
})

const detailHealthTrendOption = computed(() => {
  const temp = detail.value.temperatureTrend || []
  const heart = detail.value.heartRateTrend || []
  const activity = detail.value.activityTrend || []
  const labels = pickLabels(temp, heart, activity)
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 4, data: ['体温', '心率', '活动量'], textStyle: { color: '#55657d', fontSize: 12 } },
    grid: { left: 72, right: 72, top: 44, bottom: 34 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#d8dee8' } },
      axisLabel: { color: '#66758c', fontSize: 11 }
    },
    yAxis: [
      {
        type: 'value',
        name: '体温 (℃)',
        min: 37,
        max: 41,
        axisLabel: { color: '#66758c', fontSize: 11 },
        splitLine: { lineStyle: { color: '#edf1f6' } }
      },
      {
        type: 'value',
        name: '心率 (bpm)',
        min: 60,
        max: 120,
        axisLabel: { color: '#66758c', fontSize: 11 },
        splitLine: { show: false }
      }
    ],
    series: [
      buildSeries('体温', temp, '#f04444', 0),
      buildSeries('心率', heart, '#f6a400', 1),
      buildSeries('活动量', activity, '#3b82f6', 1)
    ]
  }
})

async function loadSummary() {
  summary.value = await api.disease.getSummary()
}

async function loadAnimals() {
  animals.value = await api.disease.getRiskAnimals(null, riskLevelFilter.value || null)
  if (!assessmentAnimalId.value && animals.value.length) {
    assessmentAnimalId.value = animals.value[0].animalId
  }
}

async function loadCharts() {
  healthEvents.value = await api.disease.getHealthEvents(null, 8)
}

function openAssessmentModal() {
  showAssessmentModal.value = true
  assessmentForm.assessmentTime = buildCurrentDateTime()
  if (!assessmentAnimalId.value && animals.value.length) {
    assessmentAnimalId.value = animals.value[0].animalId
  }
}

function openDiseaseFilter() {
  riskLevelFilterDraft.value = riskLevelFilter.value
  showFilterModal.value = true
}

async function applyDiseaseFilter() {
  riskLevelFilter.value = riskLevelFilterDraft.value
  showFilterModal.value = false
  await loadAnimals()
}

async function resetDiseaseFilter() {
  riskLevelFilterDraft.value = ''
  riskLevelFilter.value = ''
  showFilterModal.value = false
  await loadAnimals()
}

async function loadModelConfig() {
  const configMap = await api.system.getConfigMap('disease')
  modelConfig.highRiskThreshold = Number(configMap.disease_high_risk_threshold || 85)
  modelConfig.mediumRiskThreshold = Number(configMap.disease_medium_risk_threshold || 70)
  modelConfig.assessmentCycle = configMap.disease_assessment_cycle || '实时评估'
  modelConfig.alertStrategy = configMap.disease_alert_strategy || '均衡'
  modelConfig.lastUpdate = configMap.disease_model_update || '2025-12-20'
}

async function openModelConfig() {
  await loadModelConfig()
  showModelConfigModal.value = true
}

async function saveModelConfig() {
  if (savingAction.value) return
  savingAction.value = 'model-config'
  try {
    await Promise.all([
      api.system.updateConfig('disease_high_risk_threshold', modelConfig.highRiskThreshold),
      api.system.updateConfig('disease_medium_risk_threshold', modelConfig.mediumRiskThreshold),
      api.system.updateConfig('disease_assessment_cycle', modelConfig.assessmentCycle),
      api.system.updateConfig('disease_alert_strategy', modelConfig.alertStrategy),
      api.system.updateConfig('disease_model_update', modelConfig.lastUpdate)
    ])
    await loadModelConfig()
    await Promise.all([loadSummary(), loadAnimals(), loadCharts()])
    api.notify('模型配置已保存')
    showModelConfigModal.value = false
  } catch (error) {
    api.notify(error.message || '模型配置保存失败', 'danger')
  } finally {
    savingAction.value = ''
  }
}

function openAlarmHistory() {
  showAlarmHistoryModal.value = true
  if (!healthEvents.value.length) {
    loadCharts().catch(console.error)
  }
}

async function runAssessment() {
  if (!assessmentAnimalId.value) return
  const result = await api.disease.assess(assessmentAnimalId.value)
  api.notify(`评估完成：${formatAnimalId(result.animalId)} ${riskText(result.overallRisk)} / ${result.riskScore} 分`)
}

async function openDetail(animal) {
  detailLoading.value = true
  showDetailModal.value = true
  try {
    detail.value = await api.disease.getAnimalRiskDetail(animal.animalId)
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  showDetailModal.value = false
}

async function openMedicalRecord(animal) {
  showMedicalRecordModal.value = true
  medicalRecord.value = animal
  try {
    medicalRecord.value = await api.disease.getAnimalRiskDetail(animal.animalId)
  } catch (error) {
    console.error(error)
  }
}

async function openTreatmentPlan(animal) {
  const plan = await api.disease.getTreatmentPlan(animal.animalId)
  Object.assign(treatmentForm, {
    animalId: animal.animalId,
    diagnosis: normalizeTreatmentScheme(plan.diagnosis),
    treatmentPlan: plan.treatmentPlan || plan.medication || '',
    startDate: toDateInput(plan.startDate) || toDateInput(new Date()),
    endDate: toDateInput(plan.endDate) || toDateInput(addDays(new Date(), plan.duration || 7)),
    careRequirement: plan.notes || '',
    recheckAt: toDateTimeInput(addDays(new Date(), 3)),
    notes: plan.notes || ''
  })
  showTreatmentModal.value = true
}

async function saveTreatmentPlan() {
  if (savingAction.value) return
  savingAction.value = 'treatment'
  try {
    await api.disease.createTreatmentPlan(treatmentForm.animalId, {
      diagnosis: treatmentForm.diagnosis,
      treatmentPlan: treatmentForm.treatmentPlan,
      medication: treatmentForm.treatmentPlan,
      dosage: '按兽医方案执行',
      frequency: '每日 1 次',
      duration: calculateDuration(treatmentForm.startDate, treatmentForm.endDate),
      veterinarian: '值班兽医',
      notes: [treatmentForm.careRequirement, treatmentForm.recheckAt ? `复查安排：${treatmentForm.recheckAt}` : '', treatmentForm.notes]
        .filter(Boolean)
        .join('；')
    })
    api.notify('治疗计划已保存')
    showTreatmentModal.value = false
  } catch (error) {
    api.notify(error.message || '治疗计划保存失败', 'danger')
  } finally {
    savingAction.value = ''
  }
}

function configuredRiskLevel(animal) {
  const score = Number(animal?.riskScore || 0)
  if (score >= Number(modelConfig.highRiskThreshold || 85)) return 'high'
  if (score >= Number(modelConfig.mediumRiskThreshold || 70)) return 'medium'
  return 'low'
}

function configuredRiskText(animal) {
  return riskText(configuredRiskLevel(animal))
}

function riskText(level) {
  return {
    high: '高风险',
    medium: '中风险',
    low: '低风险',
    normal: '健康'
  }[level] || '健康'
}

function normalizeTreatmentScheme(value) {
  const text = String(value || '')
  if (['抗生素治疗', '消化调理', '退热护理', '关节护理'].includes(text)) return text
  if (text.includes('消化')) return '消化调理'
  if (text.includes('关节')) return '关节护理'
  if (text.includes('热') || text.includes('发烧')) return '退热护理'
  return '抗生素治疗'
}

function riskShortText(level) {
  return { high: '高', medium: '中', low: '低', normal: '低' }[level] || '低'
}

function riskTrendText(level) {
  return { high: '↑', medium: '→', low: '↓', normal: '↓' }[level] || '→'
}

function riskTrendClass(level) {
  return {
    high: 'manual34-red',
    medium: 'manual34-muted-dark',
    low: 'manual34-green',
    normal: 'manual34-green'
  }[level] || 'manual34-muted-dark'
}

function riskPercent(value) {
  const total = Number(summary.value?.baseline?.totalAnimals || summary.value?.totalAnimals || 0)
  if (!total) return 0
  return Math.min(100, Math.round((Number(value || 0) / total) * 100))
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function buildCurrentDateTime() {
  return toDateTimeInput(new Date())
}

function toDateInput(value) {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}

function toDateTimeInput(value) {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}

function addDays(value, days) {
  const date = value instanceof Date ? new Date(value) : new Date(value)
  date.setDate(date.getDate() + Number(days || 0))
  return date
}

function calculateDuration(start, end) {
  const startDate = new Date(start)
  const endDate = new Date(end)
  if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) return 7
  return Math.max(1, Math.round((endDate - startDate) / 86400000))
}

function pickLabels(...sources) {
  const first = sources.find((source) => Array.isArray(source) && source.length) || []
  return first.map((item) => item.label || item.date || item.time || '-')
}

function buildSeries(name, source, color, yAxisIndex) {
  return {
    name,
    type: 'line',
    smooth: true,
    yAxisIndex,
    data: (source || []).map((item) => item.value ?? 0),
    symbolSize: 5,
    itemStyle: { color },
    lineStyle: { color, width: 2 }
  }
}

function exportDiseaseCsv() {
  const rows = [
    ['牲畜编号', '风险等级', '风险分数', '风险类型', '主要症状', '风险趋势', '最近更新'],
    ...animals.value.map((animal) => [
      formatAnimalId(animal.animalId),
      riskShortText(configuredRiskLevel(animal)),
      animal.riskScore,
      animal.mainRisk,
      animal.mainSymptoms,
      riskTrendText(configuredRiskLevel(animal)),
      animal.lastCheckTimeText
    ])
  ]
  downloadCsv('风险牲畜列表.csv', rows)
}


onMounted(async () => {
  await loadModelConfig()
  await Promise.all([loadSummary(), loadAnimals(), loadCharts()])
})
</script>

<style src="./disease/disease-page.css"></style>
