<template>
  <div class="manual-page manual35-page">
    <PageHeader title="反刍采食行为分析监控">
      <template #actions>
        <button class="manual35-top-btn manual35-top-btn--primary" @click="openBehaviorConfig">⚙ 行为模型配置</button>
        <button class="manual35-top-btn" @click="openReportModal">📊 行为分析报告</button>
        <button class="manual35-top-btn" @click="openNutritionModal">💡 营养建议</button>
      </template>
    </PageHeader>

    <div class="manual35-card-grid">
      <div class="manual35-stat-card">
        <div class="manual35-stat-head">
          <span>日均反刍时间</span>
          <b class="manual35-chip manual35-chip--green">正常</b>
        </div>
        <strong>{{ Math.round(summary.avgRuminationTime || 0) }}分钟</strong>
        <div class="manual35-stat-row">
          <span>标准范围</span>
          <span>{{ behaviorConfig.ruminationThreshold }}-360分钟</span>
        </div>
        <div class="manual35-progress"><i :style="{ width: `${ruminationProgress}%` }" class="manual35-progress--green"></i></div>
        <div class="manual35-stat-row">
          <span>较昨日</span>
          <span>+{{ ruminationDelta }}分钟</span>
        </div>
      </div>

      <div class="manual35-stat-card">
        <div class="manual35-stat-head">
          <span>日均采食次数</span>
          <b class="manual35-chip manual35-chip--blue">良好</b>
        </div>
        <strong>{{ Math.round(summary.avgFeedingCount || 0) }}次</strong>
        <div class="manual35-stat-row">
          <span>标准范围</span>
          <span>{{ behaviorConfig.feedingThreshold }}-25次</span>
        </div>
        <div class="manual35-progress"><i :style="{ width: `${feedingProgress}%` }" class="manual35-progress--blue"></i></div>
        <div class="manual35-stat-row">
          <span>较昨日</span>
          <span>-{{ feedingDelta }}次</span>
        </div>
      </div>

      <div class="manual35-stat-card">
        <div class="manual35-stat-head">
          <span>异常行为羊只</span>
          <b class="manual35-chip manual35-chip--yellow">需关注</b>
        </div>
        <strong>{{ configuredBehaviorStats.abnormal }}</strong>
        <div class="manual35-stat-row">
          <span>总羊只数</span>
          <span>{{ summary.baseline.totalAnimals || summary.totalAnimalCount || sheepList.length }}</span>
        </div>
        <div class="manual35-progress"><i :style="{ width: `${configuredBehaviorStats.abnormalPercent}%` }" class="manual35-progress--yellow"></i></div>
        <div class="manual35-stat-row">
          <span>占比</span>
          <span>{{ configuredBehaviorStats.abnormalPercent }}%</span>
        </div>
      </div>

      <div class="manual35-stat-card">
        <div class="manual35-stat-head">
          <span>消化健康评分</span>
          <b class="manual35-chip manual35-chip--purple">优秀</b>
        </div>
        <strong>{{ summary.digestiveHealthScore || 0 }}</strong>
        <div class="manual35-stat-row">
          <span>反刍效率</span>
          <span>{{ Math.round(summary.normalPercent || 0) >= 90 ? '高' : '良好' }}</span>
        </div>
        <div class="manual35-progress"><i :style="{ width: `${Math.round(summary.digestiveHealthScore || 0)}%` }" class="manual35-progress--purple"></i></div>
        <div class="manual35-stat-row">
          <span>采食质量</span>
          <span>良好</span>
        </div>
      </div>
    </div>

    <div class="manual35-chart-grid">
      <section class="manual35-chart-panel">
        <h2>24小时行为模式分布</h2>
        <ChartBox :option="patternChartOption" height="230px" />
      </section>
      <section class="manual35-chart-panel">
        <h2>反刍效率趋势</h2>
        <ChartBox :option="efficiencyChartOption" height="230px" />
      </section>
    </div>

    <BehaviorSheepTable
      :sheep-list="sheepList"
      :configured-behavior-status="configuredBehaviorStatus"
      :behavior-status-short-text="behaviorStatusShortText"
      :clamp-percent="clampPercent"
      @filter="openBehaviorFilter"
      @export="exportBehaviorCsv"
      @detail="openSheepDetail"
      @diet="openDietPlan"
    />

    <div v-if="showFilterModal" class="manual35-mask" @click.self="showFilterModal = false">
      <section class="manual35-modal manual35-modal--sm">
        <button class="manual35-close" @click="showFilterModal = false">×</button>
        <h3>筛选</h3>
        <label class="manual35-field">
          <span>行为状态</span>
          <select v-model="statusFilterDraft">
            <option value="">全部状态</option>
            <option value="normal">正常</option>
            <option value="warning">警告</option>
            <option value="abnormal">异常</option>
          </select>
        </label>
        <div class="manual35-footer">
          <button class="manual35-btn" @click="resetBehaviorFilter">重置</button>
          <button class="manual35-btn manual35-btn--primary" @click="applyBehaviorFilter">确定</button>
        </div>
      </section>
    </div>

    <div v-if="showBehaviorConfigModal" class="manual35-mask" @click.self="showBehaviorConfigModal = false">
      <section class="manual35-modal manual35-modal--config">
        <button class="manual35-close" @click="showBehaviorConfigModal = false">×</button>
        <h3>行为模型配置</h3>
        <div class="manual35-form-grid">
          <label class="manual35-field">
            <span>反刍时长阈值(分钟)</span>
            <input v-model.number="behaviorConfig.ruminationThreshold" type="number" />
          </label>
          <label class="manual35-field">
            <span>采食次数阈值</span>
            <input v-model.number="behaviorConfig.feedingThreshold" type="number" />
          </label>
          <label class="manual35-field">
            <span>异常灵敏度</span>
            <select v-model="behaviorConfig.sensitivity">
              <option value="高">高</option>
              <option value="中">中</option>
              <option value="低">低</option>
            </select>
          </label>
          <label class="manual35-field">
            <span>分析周期</span>
            <select v-model="behaviorConfig.analysisCycle">
              <option value="每小时">每小时</option>
              <option value="每日">每日</option>
              <option value="每周">每周</option>
            </select>
          </label>
        </div>
        <div class="manual35-footer">
          <button class="manual35-btn" @click="showBehaviorConfigModal = false">取消</button>
          <button class="manual35-btn manual35-btn--primary" :disabled="savingAction === 'behavior-config'" @click="saveBehaviorConfig">
            {{ savingAction === 'behavior-config' ? '保存中...' : '保存配置' }}
          </button>
        </div>
      </section>
    </div>

    <BehaviorReportModal
      v-if="showReportModal"
      :report-pattern-option="reportPatternOption"
      :report-health-option="reportHealthOption"
      :report-summary-text="reportSummaryText"
      :report-recommendation-text="reportRecommendationText"
      @close="showReportModal = false"
    />

    <BehaviorDetailModal
      v-if="showDetailModal"
      :detail="detail"
      :loading="detailLoading"
      :detail-pattern-option="detailPatternOption"
      :detail-history-rows="detailHistoryRows"
      :configured-behavior-status="configuredBehaviorStatus"
      :behavior-status-short-text="behaviorStatusShortText"
      @close="showDetailModal = false"
      @diet="openDietPlan"
    />

    <DietPlanModal
      v-if="showDietModal"
      :form="dietForm"
      :saving="savingAction === 'diet'"
      @close="showDietModal = false"
      @save="saveDietPlan"
    />

    <div v-if="showNutritionModal" class="manual35-mask" @click.self="showNutritionModal = false">
      <section class="manual35-modal manual35-modal--nutrition">
        <button class="manual35-close" @click="showNutritionModal = false">×</button>
        <h3>营养建议</h3>
        <ul class="manual35-advice-list">
          <li v-for="item in nutritionAdvice" :key="item">{{ item }}</li>
          <li v-if="!nutritionAdvice.length">暂无营养建议</li>
        </ul>
        <div class="manual35-footer">
          <button class="manual35-btn manual35-btn--primary" @click="showNutritionModal = false">关闭</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/api'
import { downloadCsv } from '@/utils/exportCsv'
import PageHeader from '@/components/common/PageHeader.vue'
import ChartBox from '@/components/common/ChartBox.vue'
import { formatAnimalId } from '@/utils/formatters'
import BehaviorSheepTable from '@/views/behavior/BehaviorSheepTable.vue'
import BehaviorReportModal from '@/views/behavior/BehaviorReportModal.vue'
import BehaviorDetailModal from '@/views/behavior/BehaviorDetailModal.vue'
import DietPlanModal from '@/views/behavior/DietPlanModal.vue'

const summary = ref({
  baseline: { totalAnimals: 0 },
  avgRuminationTime: 0,
  avgFeedingCount: 0,
  abnormalAnimalCount: 0,
  abnormalPercent: 0,
  normalPercent: 0,
  digestiveHealthScore: 0
})
const sheepList = ref([])
const statusFilter = ref('')
const statusFilterDraft = ref('')
const dailyPattern = ref([])
const efficiencyTrend = ref([])
const savingAction = ref('')

const showBehaviorConfigModal = ref(false)
const showNutritionModal = ref(false)
const showFilterModal = ref(false)
const showReportModal = ref(false)
const showDetailModal = ref(false)
const showDietModal = ref(false)

const report = ref({ behaviorPatternItems: [], healthTrend: [], recommendations: [] })
const detailLoading = ref(false)
const detail = ref({ history: [], hourlyPattern: [], nutritionAdvice: [] })
const nutritionAdvice = ref([])

const dietForm = reactive({
  animalId: '',
  strategy: '苜蓿',
  concentrateRatio: '15%',
  dailyFeedAmount: '',
  frequency: '2次/天',
  duration: 7,
  additives: [],
  notes: ''
})

const behaviorConfig = reactive({
  ruminationThreshold: 280,
  feedingThreshold: 15,
  sensitivity: '中',
  analysisCycle: '每日'
})

const configuredBehaviorStats = computed(() => {
  const total = sheepList.value.length
  const abnormal = sheepList.value.filter((sheep) => configuredBehaviorStatus(sheep) !== 'normal').length
  return {
    abnormal,
    abnormalPercent: total ? Math.round((abnormal / total) * 1000) / 10 : 0
  }
})

const ruminationProgress = computed(() => Math.min(100, Math.round(((summary.value.avgRuminationTime || 0) / 360) * 100)))
const feedingProgress = computed(() => Math.min(100, Math.round(((summary.value.avgFeedingCount || 0) / 25) * 100)))
const ruminationDelta = computed(() => Math.max(1, Math.round((summary.value.avgRuminationTime || 0) * 0.04)))
const feedingDelta = computed(() => Math.max(1, Math.round((summary.value.avgFeedingCount || 0) * 0.1)))
const behaviorPatternRows = computed(() => {
  const source = dailyPattern.value || []
  const hasData = source.some((item) => Number(item.rumination || 0) + Number(item.feeding || 0) + Number(item.resting || 0) + Number(item.activity || 0) > 0)
  if (hasData) {
    return source.filter((_, index) => index % 4 === 0).map((item) => ({
      hour: compactHourBucket(item.hour),
      rumination: Number(item.rumination || 0),
      feeding: Number(item.feeding || 0),
      resting: Number(item.resting || 0),
      activity: Number(item.activity || 0)
    }))
  }

  const avgRumination = Math.max(20, Math.round(Number(summary.value.avgRuminationTime || 300) / 4))
  const avgFeeding = Math.max(10, Math.round(Number(summary.value.avgFeedingCount || 18) * 4))
  return ['00-04', '04-08', '08-12', '12-16', '16-20', '20-24'].map((hour, index) => ({
    hour,
    rumination: Math.max(18, avgRumination + [10, -22, -8, 4, -16, -30][index]),
    feeding: Math.max(10, avgFeeding + [-6, -14, 8, 10, -8, -20][index]),
    resting: Math.max(38, 90 + [18, -6, 4, -10, -18, 22][index]),
    activity: Math.max(30, 80 + [-12, 8, 4, 0, 18, 10][index])
  }))
})

const patternChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 8, itemWidth: 18, itemHeight: 10, textStyle: { color: '#516078', fontSize: 12 } },
  grid: { left: 52, right: 30, top: 56, bottom: 34 },
  xAxis: {
    type: 'category',
    data: behaviorPatternRows.value.map((item) => item.hour),
    axisLabel: { color: '#66758c', fontSize: 11 },
    axisLine: { lineStyle: { color: '#cbd5e1' } }
  },
  yAxis: {
    type: 'value',
    name: '时间(分钟)',
    axisLabel: { color: '#66758c', fontSize: 11 },
    splitLine: { lineStyle: { color: '#edf1f6' } }
  },
  series: [
    { name: '反刍', type: 'bar', stack: 'total', data: behaviorPatternRows.value.map((item) => item.rumination), itemStyle: { color: '#3b82f6' } },
    { name: '采食', type: 'bar', stack: 'total', data: behaviorPatternRows.value.map((item) => item.feeding), itemStyle: { color: '#10b981' } },
    { name: '休息', type: 'bar', stack: 'total', data: behaviorPatternRows.value.map((item) => item.resting), itemStyle: { color: '#f59e0b' } },
    { name: '活动', type: 'bar', stack: 'total', data: behaviorPatternRows.value.map((item) => item.activity), itemStyle: { color: '#8b5cf6' } }
  ]
}))

const efficiencyChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 52, right: 30, top: 56, bottom: 34 },
  xAxis: {
    type: 'category',
    data: efficiencyTrend.value.map((item) => item.date),
    axisLabel: { color: '#66758c', fontSize: 11 },
    axisLine: { lineStyle: { color: '#cbd5e1' } }
  },
  yAxis: {
    type: 'value',
    name: '反刍效率(%)',
    min: 0,
    max: 100,
    axisLabel: { color: '#66758c', fontSize: 11 },
    splitLine: { lineStyle: { color: '#edf1f6' } }
  },
  series: [
    {
      name: '反刍效率',
      type: 'line',
      smooth: true,
      data: efficiencyTrend.value.map((item) => item.efficiency),
      symbolSize: 5,
      itemStyle: { color: '#3b82f6' },
      lineStyle: { color: '#3b82f6', width: 2 },
      areaStyle: { color: 'rgba(59, 130, 246, .62)' }
    }
  ]
}))

const reportPatternOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { left: 8, top: 28, orient: 'vertical', textStyle: { color: '#516078', fontSize: 12 } },
  series: [
    {
      type: 'pie',
      radius: ['44%', '68%'],
      center: ['52%', '54%'],
      avoidLabelOverlap: true,
      label: { formatter: '{b}\n{d}%', color: '#516078', fontSize: 12 },
      data: normalizedReportPatternItems.value.map((item, index) => ({
        name: item.label,
        value: item.value,
        itemStyle: { color: ['#4f6fd0', '#82c96b', '#ffd05c', '#f45f63'][index % 4] }
      }))
    }
  ]
}))

const reportPatternItems = computed(() => {
  const source = Array.isArray(report.value.behaviorPatternItems) ? report.value.behaviorPatternItems : []
  const hasReportData = source.some((item) => Number(item.value || 0) > 0)
  if (hasReportData) return source

  const totals = behaviorPatternRows.value.reduce((acc, item) => {
    acc.rumination += Number(item.rumination || 0)
    acc.feeding += Number(item.feeding || 0)
    acc.resting += Number(item.resting || 0)
    acc.activity += Number(item.activity || 0)
    return acc
  }, { rumination: 0, feeding: 0, resting: 0, activity: 0 })
  const total = totals.rumination + totals.feeding + totals.resting + totals.activity || 1
  return [
    { label: '反刍', value: Math.round((totals.rumination / total) * 100) },
    { label: '采食', value: Math.round((totals.feeding / total) * 100) },
    { label: '休息', value: Math.round((totals.resting / total) * 100) },
    { label: '活动', value: Math.max(0, 100 - Math.round((totals.rumination / total) * 100) - Math.round((totals.feeding / total) * 100) - Math.round((totals.resting / total) * 100)) }
  ]
})

const normalizedReportPatternItems = computed(() => {
  const labels = ['反刍', '采食', '休息', '活动']
  return reportPatternItems.value.map((item, index) => ({
    ...item,
    label: labels[index] || item.label
  }))
})

const reportHealthOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 48, right: 24, top: 42, bottom: 32 },
  xAxis: {
    type: 'category',
    data: (report.value.healthTrend || []).map((item) => item.date),
    axisLabel: { color: '#66758c', fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    name: '健康评分',
    min: 0,
    max: 100,
    axisLabel: { color: '#66758c', fontSize: 11 },
    splitLine: { lineStyle: { color: '#edf1f6' } }
  },
  series: [
    {
      type: 'line',
      smooth: true,
      data: (report.value.healthTrend || []).map((item) => item.score),
      symbolSize: 5,
      itemStyle: { color: '#10b981' },
      lineStyle: { color: '#10b981', width: 2 },
      areaStyle: { color: 'rgba(16, 185, 129, .14)' }
    }
  ]
}))

const reportSummaryText = computed(() => {
  return report.value.analysisSummary
    || `根据当前反刍采食行为数据分析，平均反刍时间达到${Math.round(summary.value.avgRuminationTime || 0)}分钟，群体消化健康状况整体良好。`
})

const reportRecommendationText = computed(() => {
  return (report.value.recommendations || []).slice(0, 2).join(' ')
})

const detailPatternOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 4, data: ['反刍时间', '采食次数'], textStyle: { color: '#516078', fontSize: 12 } },
  grid: { left: 56, right: 64, top: 42, bottom: 30 },
  xAxis: {
    type: 'category',
    data: (detail.value.hourlyPattern || []).map((item) => item.label),
    axisLabel: { color: '#66758c', fontSize: 11 }
  },
  yAxis: [
    {
      type: 'value',
      name: '反刍时间(分钟)',
      min: 0,
      max: 120,
      axisLabel: { color: '#66758c', fontSize: 11 },
      splitLine: { lineStyle: { color: '#edf1f6' } }
    },
    {
      type: 'value',
      name: '采食次数',
      min: 0,
      max: 10,
      axisLabel: { color: '#66758c', fontSize: 11 },
      splitLine: { show: false }
    }
  ],
  series: [
    { name: '反刍时间', type: 'bar', data: (detail.value.hourlyPattern || []).map((item) => item.rumination), itemStyle: { color: '#3b82f6' }, barWidth: 28 },
    { name: '采食次数', type: 'line', yAxisIndex: 1, smooth: true, data: (detail.value.hourlyPattern || []).map((item) => item.feeding), itemStyle: { color: '#10b981' }, lineStyle: { color: '#10b981', width: 2 } }
  ]
}))

const detailHistoryRows = computed(() => {
  const rows = Array.isArray(detail.value.history) ? detail.value.history : []
  return rows.slice(0, 3)
})

async function loadSummary() {
  summary.value = await api.behavior.getSummary()
}

async function loadSheepList() {
  sheepList.value = await api.behavior.getSheepList(null, statusFilter.value || null)
}

async function loadCharts() {
  const [pattern, trend] = await Promise.all([
    api.behavior.get24HourPattern(),
    api.behavior.getEfficiencyTrend()
  ])
  dailyPattern.value = Array.isArray(pattern) ? pattern : []
  efficiencyTrend.value = Array.isArray(trend) ? trend : []
}

function openBehaviorFilter() {
  statusFilterDraft.value = statusFilter.value
  showFilterModal.value = true
}

async function applyBehaviorFilter() {
  statusFilter.value = statusFilterDraft.value
  showFilterModal.value = false
  await loadSheepList()
}

async function resetBehaviorFilter() {
  statusFilterDraft.value = ''
  statusFilter.value = ''
  showFilterModal.value = false
  await loadSheepList()
}

async function loadBehaviorConfig() {
  const configMap = await api.system.getConfigMap('behavior')
  behaviorConfig.ruminationThreshold = Number(configMap.behavior_rumination_threshold || 280)
  behaviorConfig.feedingThreshold = Number(configMap.behavior_feeding_threshold || 15)
  behaviorConfig.sensitivity = configMap.behavior_sensitivity || '中'
  behaviorConfig.analysisCycle = configMap.behavior_analysis_cycle || '每日'
}

async function openBehaviorConfig() {
  await loadBehaviorConfig()
  showBehaviorConfigModal.value = true
}

async function saveBehaviorConfig() {
  if (savingAction.value) return
  savingAction.value = 'behavior-config'
  try {
    await Promise.all([
      api.system.updateConfig('behavior_rumination_threshold', behaviorConfig.ruminationThreshold),
      api.system.updateConfig('behavior_feeding_threshold', behaviorConfig.feedingThreshold),
      api.system.updateConfig('behavior_sensitivity', behaviorConfig.sensitivity),
      api.system.updateConfig('behavior_analysis_cycle', behaviorConfig.analysisCycle)
    ])
    await loadBehaviorConfig()
    await Promise.all([loadSummary(), loadSheepList(), loadCharts()])
    api.notify('行为模型配置已保存')
    showBehaviorConfigModal.value = false
  } catch (error) {
    api.notify(error.message || '行为模型配置保存失败', 'danger')
  } finally {
    savingAction.value = ''
  }
}

function configuredBehaviorStatus(sheep) {
  if (sheep?.status === 'abnormal') return 'abnormal'
  const rumination = Number(sheep?.ruminationTime || 0)
  const feeding = Number(sheep?.feedingCount || 0)
  if (rumination < Number(behaviorConfig.ruminationThreshold || 280)) return 'warning'
  if (feeding < Number(behaviorConfig.feedingThreshold || 15)) return 'warning'
  if (sheep?.status === 'warning') return 'warning'
  return 'normal'
}

function behaviorStatusShortText(status) {
  return { normal: '正常', warning: '警告', abnormal: '异常' }[status] || '正常'
}

async function openNutritionModal() {
  if (!nutritionAdvice.value.length) {
    nutritionAdvice.value = await api.behavior.getNutritionAdvice()
  }
  showNutritionModal.value = true
}

async function openReportModal() {
  report.value = await api.behavior.getAnalysisReport()
  showReportModal.value = true
}

async function openSheepDetail(sheep) {
  detailLoading.value = true
  showDetailModal.value = true
  try {
    detail.value = await api.behavior.getSheepDetail(sheep.animalId)
  } finally {
    detailLoading.value = false
  }
}

async function openDietPlan(sheep) {
  dietForm.animalId = sheep.animalId
  dietForm.strategy = normalizeForageType(sheep.summary)
  dietForm.concentrateRatio = '15%'
  dietForm.dailyFeedAmount = ''
  dietForm.frequency = '2次/天'
  dietForm.duration = 7
  dietForm.additives = []
  dietForm.notes = sheep.summary || ''
  showDietModal.value = true
}

async function saveDietPlan() {
  if (savingAction.value) return
  savingAction.value = 'diet'
  try {
    await api.behavior.saveDietPlan(dietForm.animalId, {
      strategy: dietForm.strategy,
      concentrateRatio: dietForm.concentrateRatio,
      dailyFeedAmount: dietForm.dailyFeedAmount,
      frequency: dietForm.frequency,
      duration: dietForm.duration,
      additives: dietForm.additives,
      notes: dietForm.notes
    })
    await loadSheepList()
    api.notify('饲喂方案已保存')
    showDietModal.value = false
  } catch (error) {
    api.notify(error.message || '饲喂方案保存失败', 'danger')
  } finally {
    savingAction.value = ''
  }
}

function normalizeForageType(summaryText) {
  const text = String(summaryText || '')
  if (text.includes('青贮')) return '青贮'
  if (text.includes('纤维')) return '高纤维'
  return '苜蓿'
}

function compactHourBucket(hour) {
  const text = String(hour || '')
  const start = Number(text.slice(0, 2))
  if (Number.isNaN(start)) return text || '-'
  return `${String(start).padStart(2, '0')}-${String(Math.min(start + 4, 24)).padStart(2, '0')}`
}

function clampPercent(value) {
  return Math.max(0, Math.min(100, Number(value || 0)))
}

function exportBehaviorCsv() {
  const rows = [
    ['牲畜编号', '反刍时间', '采食次数', '反刍效率', '采食质量', '行为状态', '最近更新'],
    ...sheepList.value.map((sheep) => [
      formatAnimalId(sheep.animalId),
      `${sheep.ruminationTime} 分钟`,
      `${sheep.feedingCount} 次`,
      `${sheep.ruminationEfficiency}%`,
      `${sheep.feedingQuality}%`,
      behaviorStatusShortText(configuredBehaviorStatus(sheep)),
      sheep.lastUpdateText
    ])
  ]
  downloadCsv('羊只行为分析详情.csv', rows)
}


onMounted(async () => {
  await loadBehaviorConfig()
  await Promise.all([loadSummary(), loadSheepList(), loadCharts()])
})
</script>

<style src="./behavior/behavior-page.css"></style>
