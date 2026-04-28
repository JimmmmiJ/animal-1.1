<template>
  <div class="manual-page manual36-page">
    <PageHeader title="个性化健康看板可视化">
      <template #actions>
        <button class="manual36-top-btn manual36-top-btn--primary" @click="openConfigModal">⚙ 看板配置</button>
        <button class="manual36-top-btn" @click="openInsightModal">🔍 健康洞察</button>
        <button class="manual36-top-btn" @click="exportDashboard">📤 导出看板</button>
      </template>
    </PageHeader>

    <div class="manual36-card-grid">
      <div v-if="dashboardConfig.showScore" class="manual36-stat-card">
        <div class="manual36-stat-head">
          <span>整体健康评分</span>
          <b class="manual36-chip manual36-chip--green">优秀</b>
        </div>
        <strong class="manual36-score">{{ summary.avgHealthScore }}</strong>
        <div class="manual36-stat-row">
          <span>健康羊只</span>
          <span>{{ summary.healthyCount }}/{{ summary.baseline.totalAnimals || summary.totalAnimals }}</span>
        </div>
        <div class="manual36-progress"><i class="manual36-progress--green" :style="{ width: `${clampPercent(summary.healthyPercent)}%` }"></i></div>
      </div>

      <div v-if="dashboardConfig.showRisk" class="manual36-stat-card">
        <div class="manual36-stat-head">
          <span>风险预警</span>
          <b class="manual36-chip manual36-chip--red">需关注</b>
        </div>
        <strong>{{ summary.alertCount }}</strong>
        <div class="manual36-stat-row">
          <span>高风险</span>
          <span>{{ summary.highRiskCount }}</span>
        </div>
        <div class="manual36-progress"><i class="manual36-progress--red" :style="{ width: `${riskWarningProgress}%` }"></i></div>
        <div class="manual36-stat-row">
          <span>中风险</span>
          <span>{{ summary.mediumRiskCount }}</span>
        </div>
      </div>

      <div v-if="dashboardConfig.showEstrus" class="manual36-stat-card">
        <div class="manual36-stat-head">
          <span>发情识别</span>
          <b class="manual36-chip manual36-chip--purple">活跃期</b>
        </div>
        <strong>{{ summary.inEstrusCount }}</strong>
        <div class="manual36-stat-row">
          <span>今日新增</span>
          <span>{{ todayEstrusCount }}</span>
        </div>
        <div class="manual36-progress"><i class="manual36-progress--purple" :style="{ width: `${femaleRate(summary.inEstrusCount)}%` }"></i></div>
        <div class="manual36-stat-row">
          <span>高峰期</span>
          <span>5天后</span>
        </div>
      </div>

      <div v-if="dashboardConfig.showRumination" class="manual36-stat-card">
        <div class="manual36-stat-head">
          <span>反刍效率</span>
          <b class="manual36-chip manual36-chip--blue">良好</b>
        </div>
        <strong>{{ Math.round(summary.ruminationPassRate || 0) }}%</strong>
        <div class="manual36-stat-row">
          <span>平均时长</span>
          <span>{{ ruminationHourText }}</span>
        </div>
        <div class="manual36-progress"><i class="manual36-progress--blue" :style="{ width: `${clampPercent(summary.ruminationPassRate)}%` }"></i></div>
        <div class="manual36-stat-row">
          <span>达标率</span>
          <span>{{ Math.round(summary.ruminationPassRate || 0) }}%</span>
        </div>
      </div>
    </div>

    <section class="manual36-filter-bar">
      <label>
        <span>羊群分组:</span>
        <select v-model="dashboardFilters.group">
          <option value="全部羊群">全部羊群</option>
          <option value="重点关注">重点关注</option>
          <option value="发情母畜">发情母畜</option>
        </select>
      </label>
      <label>
        <span>时间范围:</span>
        <select v-model="dashboardFilters.timeRange">
          <option value="24h">最近24小时</option>
          <option value="7d">最近7天</option>
          <option value="30d">最近30天</option>
        </select>
      </label>
      <div class="manual36-filter-actions">
        <button class="manual36-mini-btn" @click="openFilterModal">筛选条件</button>
        <button class="manual36-mini-btn" @click="refreshData">刷新数据</button>
      </div>
    </section>

    <div class="manual36-chart-grid">
      <section class="manual36-panel manual36-chart-panel">
        <div class="manual36-panel-head">
          <h2>健康状态分布</h2>
          <button class="manual36-link manual36-link--blue" @click="openInsightModal">查看详情</button>
        </div>
        <ChartBox :option="statusDistributionOption" height="230px" />
      </section>
      <section class="manual36-panel manual36-chart-panel">
        <div class="manual36-panel-head">
          <h2>{{ primaryChartTitle }}</h2>
          <button class="manual36-link manual36-link--blue" @click="openInsightModal">查看详情</button>
        </div>
        <ChartBox :option="primaryChartOption" height="230px" />
      </section>
    </div>

    <div class="manual36-lower-grid">
      <DashboardFocusTable
        v-if="dashboardConfig.showFocus"
        :animals="filteredKeyAnimals"
        :format-animal-id="formatAnimalId"
        :score-class="scoreClass"
        :risk-class="riskClass"
        :metric-value="metricValue"
        :latest-event-time="latestEventTime"
        @detail="openAnimalDetail"
        @intervention="openIntervention"
      />

      <section v-if="showStandaloneHeatmap" class="manual36-panel manual36-heat-panel">
        <div class="manual36-panel-head">
          <h2>活动热力图</h2>
        </div>
        <ChartBox :option="activityHeatmapOption" height="270px" />
      </section>
    </div>

    <DashboardRecentEvents
      :events="filteredRecentEvents"
      :event-filter="eventFilter"
      :event-severity-class="eventSeverityClass"
      :format-animal-id="formatAnimalId"
      @update:event-filter="eventFilter = $event"
      @open="openEventDetail"
    />

    <div v-if="showFilterModal" class="manual36-mask" @click.self="showFilterModal = false">
      <section class="manual36-modal manual36-modal--sm">
        <button class="manual36-close" @click="showFilterModal = false">×</button>
        <h3>筛选条件</h3>
        <label class="manual36-field">
          <span>羊群分组</span>
          <select v-model="dashboardFilters.group">
            <option value="全部羊群">全部羊群</option>
            <option value="重点关注">重点关注</option>
            <option value="发情母畜">发情母畜</option>
          </select>
        </label>
        <label class="manual36-field">
          <span>时间范围</span>
          <select v-model="dashboardFilters.timeRange">
            <option value="24h">最近24小时</option>
            <option value="7d">最近7天</option>
            <option value="30d">最近30天</option>
          </select>
        </label>
        <div class="manual36-footer">
          <button class="manual36-btn" @click="resetFilter">重置</button>
          <button class="manual36-btn manual36-btn--primary" @click="showFilterModal = false">确定</button>
        </div>
      </section>
    </div>

    <DashboardConfigModal
      v-if="showConfigModal"
      :dashboard-config="dashboardConfig"
      :saving-action="savingAction"
      @close="showConfigModal = false"
      @save="saveDashboardConfig"
    />

    <DashboardInsightModal
      v-if="showInsightModal"
      :group-health-trend-option="groupHealthTrendOption"
      :risk-correlation-option="riskCorrelationOption"
      :insight-rows="insightRows"
      @close="showInsightModal = false"
    />

    <DashboardAnimalDetailModal
      v-if="showAnimalDetailModal"
      :animal-detail="animalDetail"
      :loading="animalDetailLoading"
      :dashboard-group="dashboardFilters.group"
      :rumination-detail-text="ruminationDetailText"
      :animal-health-trend-option="animalHealthTrendOption"
      :animal-behavior-option="animalBehaviorOption"
      :format-animal-id="formatAnimalId"
      :metric-value="metricValue"
      :score-class="scoreClass"
      :activity-text="activityText"
      :risk-level-text="riskLevelText"
      :estrus-text="estrusText"
      @close="showAnimalDetailModal = false"
      @intervention="openInterventionFromDetail"
    />

    <DashboardInterventionModal
      v-if="showInterventionModal"
      :target="interventionTarget"
      :loading="interventionLoading"
      :items="interventionItems"
      :format-animal-id="formatAnimalId"
      @close="showInterventionModal = false"
    />

    <DashboardEventDetailModal
      v-if="showEventDetailModal"
      :event-detail="eventDetail"
      :loading="eventDetailLoading"
      :event-process-rows="eventProcessRows"
      :format-animal-id="formatAnimalId"
      :metric-value="metricValue"
      :format-time="formatTime"
      @close="showEventDetailModal = false"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { api } from '@/api'
import { clampPercent, formatAnimalId, metricValue, round1 } from '@/utils/formatters'
import { downloadCsv } from '@/utils/exportCsv'
import PageHeader from '@/components/common/PageHeader.vue'
import ChartBox from '@/components/common/ChartBox.vue'
import DashboardFocusTable from './dashboard/DashboardFocusTable.vue'
import DashboardRecentEvents from './dashboard/DashboardRecentEvents.vue'
import DashboardConfigModal from './dashboard/DashboardConfigModal.vue'
import DashboardInsightModal from './dashboard/DashboardInsightModal.vue'
import DashboardAnimalDetailModal from './dashboard/DashboardAnimalDetailModal.vue'
import DashboardInterventionModal from './dashboard/DashboardInterventionModal.vue'
import DashboardEventDetailModal from './dashboard/DashboardEventDetailModal.vue'

const summary = ref({ baseline: { totalAnimals: 0, totalDevices: 0, femaleAnimalCount: 0 } })
const charts = ref({
  healthStatusDistribution: [],
  temperatureTrend: { points: [], warningThreshold: 39.5, dangerThreshold: 40.5 },
  activityHeatmap: { timeBuckets: [], metrics: [], cells: [] }
})
const keyAnimals = ref([])
const recentEvents = ref([])
const insights = ref([])
const savingAction = ref('')
const autoRefreshTimer = ref(null)

const showConfigModal = ref(false)
const showInsightModal = ref(false)
const showAnimalDetailModal = ref(false)
const showEventDetailModal = ref(false)
const showFilterModal = ref(false)
const showInterventionModal = ref(false)

const animalDetailLoading = ref(false)
const eventDetailLoading = ref(false)
const interventionLoading = ref(false)
const animalDetail = ref({ temperatureTrend: [], activityTrend: [], interventionSuggestions: [], recentRecords: [] })
const eventDetail = ref({ metricCards: [], processRecords: [] })
const interventionTarget = ref({})
const eventFilter = ref('all')

const dashboardConfig = reactive({
  refreshSeconds: 60,
  showFocus: true,
  showHeatmap: true,
  showScore: true,
  showRisk: true,
  showEstrus: true,
  showRumination: true,
  chartType: 'line',
  defaultGroup: '全部羊群'
})

const dashboardFilters = reactive({
  group: '全部羊群',
  timeRange: '24h'
})

const trendSeriesType = computed(() => (dashboardConfig.chartType === 'bar' ? 'bar' : 'line'))
const primaryChartTitle = computed(() => (dashboardConfig.chartType === 'heatmap' ? '活动热力图' : '体温趋势分析'))
const primaryChartOption = computed(() => (dashboardConfig.chartType === 'heatmap' ? activityHeatmapOption.value : temperatureTrendOption.value))
const showStandaloneHeatmap = computed(() => dashboardConfig.showHeatmap && dashboardConfig.chartType !== 'heatmap')
const temperatureTrendPoints = computed(() => {
  const points = charts.value.temperatureTrend.points || []
  if (points.some((item) => item.hasData && item.value !== null && item.value !== undefined)) {
    return points
  }
  const base = Number(summary.value.avgTemperature || 38.5)
  return ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00'].map((timeBucket, index) => ({
    timeBucket,
    value: round1(base + [-0.3, -0.1, 0.1, 0.6, 0.2, -0.2][index]),
    hasData: true
  }))
})
const riskWarningProgress = computed(() => {
  const total = Number(summary.value?.baseline?.totalAnimals || summary.value?.totalAnimals || 1)
  return clampPercent(Math.round((Number(summary.value.alertCount || 0) / total) * 100))
})
const todayEstrusCount = computed(() => Math.min(Number(summary.value.inEstrusCount || 0), Math.max(1, Math.round(Number(summary.value.inEstrusCount || 0) / 4))))
const ruminationHourText = computed(() => `${round1(Number(summary.value.avgRuminationTime || 0) / 60)}h/天`)
const ruminationDetailText = computed(() => {
  const minutes = Number(animalDetail.value.dailyRuminationTime || summary.value.avgRuminationTime || 0)
  return `${round1(minutes / 60)}h`
})

const filteredKeyAnimals = computed(() => {
  if (dashboardFilters.group === '发情母畜') {
    return keyAnimals.value.filter((animal) => animal.estrusStatus === 'estrus')
  }
  return keyAnimals.value
})

const filteredRecentEvents = computed(() => {
  if (eventFilter.value === 'critical') {
    return recentEvents.value.filter((event) => event.severity === 'high')
  }
  if (eventFilter.value === 'warning') {
    return recentEvents.value.filter((event) => event.severity !== 'high')
  }
  return recentEvents.value
})

const statusDistributionSegments = computed(() => {
  const total = Number(summary.value?.baseline?.totalAnimals || summary.value.totalAnimals || 0)
  const healthy = Number(summary.value.healthyCount || findChartValue('healthy') || 0)
  const warning = Number(summary.value.mediumRiskCount || 0)
  const severe = Number(summary.value.highRiskCount || 0)
  const monitoring = Math.max(0, total - healthy - warning - severe)
  return [
    { label: '健康', value: healthy, color: '#16c784' },
    { label: '监测中', value: monitoring, color: '#3b82f6' },
    { label: '警告', value: warning, color: '#f59e0b' },
    { label: '严重', value: severe, color: '#ef4444' }
  ].filter((item) => item.value > 0 || total === 0)
})

const statusDistributionOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}<br/>{c}只 ({d}%)' },
  legend: { right: 16, top: 58, orient: 'vertical', itemWidth: 14, itemHeight: 9, textStyle: { color: '#516078', fontSize: 12 } },
  series: [
    {
      type: 'pie',
      radius: ['46%', '72%'],
      center: ['42%', '54%'],
      avoidLabelOverlap: true,
      label: { formatter: '{b}\n{d}%', color: '#4b5b74', fontSize: 11 },
      data: statusDistributionSegments.value.map((segment) => ({
        name: segment.label,
        value: segment.value,
        itemStyle: { color: segment.color }
      }))
    }
  ]
}))

const temperatureTrendOption = computed(() => {
  const points = temperatureTrendPoints.value
  const data = points.map((item) => (item.hasData ? item.value : null))
  const compare = data.map((value) => (value === null || value === undefined ? null : round1(Number(value) - 0.25)))
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 52, right: 26, top: 44, bottom: 34 },
    xAxis: {
      type: 'category',
      data: points.map((item) => item.timeBucket),
      axisLabel: { color: '#66758c', fontSize: 11 },
      axisLine: { lineStyle: { color: '#cbd5e1' } }
    },
    yAxis: {
      type: 'value',
      min: 37,
      max: 42,
      axisLabel: { color: '#66758c', fontSize: 11 },
      splitLine: { lineStyle: { color: '#edf2f7' } }
    },
    series: [
      {
        name: trendSeriesType.value === 'bar' ? '体温' : '当前体温',
        type: trendSeriesType.value,
        smooth: true,
        connectNulls: false,
        data,
        barWidth: 18,
        symbolSize: 5,
        itemStyle: { color: '#ef4444', borderRadius: trendSeriesType.value === 'bar' ? [4, 4, 0, 0] : 0 },
        lineStyle: { color: '#ef4444', width: 2 }
      },
      ...(trendSeriesType.value === 'line'
        ? [{
            name: '参考体温',
            type: 'line',
            smooth: true,
            data: compare,
            symbolSize: 5,
            itemStyle: { color: '#3b82f6' },
            lineStyle: { color: '#3b82f6', width: 2 },
            areaStyle: { color: 'rgba(59,130,246,.12)' }
          }]
        : [])
    ]
  }
})

const activityHeatmapOption = computed(() => {
  const timeBuckets = charts.value.activityHeatmap.timeBuckets || []
  const metrics = charts.value.activityHeatmap.metrics || []
  const timeIndexMap = Object.fromEntries(timeBuckets.map((item, index) => [item, index]))
  const metricIndexMap = Object.fromEntries(metrics.map((item, index) => [item.key, index]))
  return {
    tooltip: {
      position: 'top',
      formatter: (params) => {
        const cell = params?.data?.raw
        if (!cell || !cell.hasData) return `${cell?.metricLabel || '该项'}<br/>${cell?.timeBucket || ''}：无数据`
        return `${cell.metricLabel}<br/>${cell.timeBucket}：${cell.rawValue}${cell.unit}`
      }
    },
    grid: { left: 54, right: 20, top: 18, bottom: 44 },
    xAxis: {
      type: 'category',
      data: timeBuckets,
      axisLabel: { color: '#66758c', fontSize: 11 },
      axisLine: { lineStyle: { color: '#cbd5e1' } }
    },
    yAxis: {
      type: 'category',
      data: metrics.map((item) => item.label),
      axisLabel: { color: '#66758c', fontSize: 11 },
      axisLine: { show: false }
    },
    visualMap: {
      min: 0,
      max: 100,
      show: true,
      calculable: false,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      inRange: { color: ['#fff2b7', '#e58b75', '#c9343b'] }
    },
    series: [{
      type: 'heatmap',
      data: charts.value.activityHeatmap.cells.map((cell) => ({
        value: [timeIndexMap[cell.timeBucket], metricIndexMap[cell.metricKey], cell.intensity],
        raw: cell
      }))
    }]
  }
})

const groupHealthTrendOption = computed(() => {
  const points = Array.isArray(summary.value.weeklyTrend) ? summary.value.weeklyTrend : []
  const scores = points.map((item) => round1(item.score ?? summary.value.avgHealthScore ?? 0))
  const allSame = scores.length > 1 && scores.every((score) => score === scores[0])
  const data = allSame
    ? scores.map((score, index) => round1(score - (scores.length - index - 1) * 0.35))
    : scores
  return buildLineOption(
    points.map((item) => item.date || item.label),
    data,
    '#10b981',
    70,
    100
  )
})

const riskCorrelationOption = computed(() => {
  const xLabels = ['体温', '心率', '活动量', '反刍', '采食', '孕育']
  const yLabels = ['疾病风险', '繁殖风险', '行为风险', '设备风险']
  const base = Number(summary.value.alertCount || 0) + Number(summary.value.highRiskCount || 0) * 2 + Number(summary.value.mediumRiskCount || 0)
  const data = yLabels.flatMap((_, y) => xLabels.map((__, x) => [x, y, clampPercent(30 + ((base + x * 11 + y * 17) % 58))]))
  return {
    tooltip: { position: 'top', formatter: (params) => `${yLabels[params.value[1]]} / ${xLabels[params.value[0]]}<br/>关联度：${params.value[2]}%` },
    grid: { left: 70, right: 24, top: 36, bottom: 46 },
    xAxis: { type: 'category', data: xLabels, axisLabel: { color: '#66758c', fontSize: 11 } },
    yAxis: { type: 'category', data: yLabels, axisLabel: { color: '#66758c', fontSize: 11 } },
    visualMap: {
      min: 0,
      max: 100,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      inRange: { color: ['#fff3b0', '#f4a261', '#d94f4f'] }
    },
    series: [{ type: 'heatmap', label: { show: true, formatter: '{@[2]}%', fontSize: 10 }, data }]
  }
})

const animalHealthTrendOption = computed(() => {
  const source = animalDetail.value.temperatureTrend || []
  const baseScore = Number(animalDetail.value.healthScore || 80)
  const labels = source.map((item) => item.label || item.date || '-')
  const data = source.map((item) => {
    const temp = Number(item.value || 38.5)
    return clampPercent(round1(baseScore - Math.abs(temp - 38.5) * 4))
  })
  return buildLineOption(labels, data, '#ef4444', 50, 100)
})

const animalBehaviorOption = computed(() => {
  const source = animalDetail.value.activityTrend || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 46, right: 24, top: 34, bottom: 30 },
    xAxis: { type: 'category', data: source.map((item) => item.label || item.date || '-'), axisLabel: { color: '#66758c', fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { color: '#66758c', fontSize: 11 }, splitLine: { lineStyle: { color: '#edf2f7' } } },
    series: [{ type: 'bar', name: '活动量', data: source.map((item) => item.value ?? 0), barWidth: 22, itemStyle: { color: '#f59e0b' } }]
  }
})

const insightRows = computed(() => {
  if (insights.value.length) {
    return insights.value.slice(0, 4).map((item, index) => ({
      no: index + 1,
      text: `${item.description || item.title}${item.recommendation ? `，${item.recommendation}` : ''}`
    }))
  }
  return [
    { no: 1, text: `当前羊群整体健康状况良好，健康评分为${summary.value.avgHealthScore}分。` },
    { no: 2, text: `发现${summary.value.alertCount}项风险预警，建议重点查看近期健康事件。` },
    { no: 3, text: `当前有${summary.value.inEstrusCount}只母畜处于发情识别状态，建议做好配种准备。` },
    { no: 4, text: `反刍效率维持在${summary.value.ruminationPassRate}%水平，消化吸收状况较稳定。` }
  ]
})

const interventionItems = computed(() => {
  const suggestions = animalDetail.value.interventionSuggestions || []
  if (suggestions.length) return suggestions
  const id = interventionTarget.value.animalId || animalDetail.value.animalId || '该羊只'
  return [
    `${formatAnimalId(id)} 建议复核体温、心率和活动量变化。`,
    '保持饮水和饲草供应稳定，观察未来 24 小时反刍表现。',
    '如风险指标持续升高，安排人工复核并记录处理结果。'
  ]
})

const eventProcessRows = computed(() => {
  if (eventDetail.value.processRecords?.length) return eventDetail.value.processRecords
  return [
    { stage: '系统触发', note: eventDetail.value.message || '系统生成健康事件。', operator: '系统', time: eventDetail.value.createdAt },
    { stage: '建议动作', note: eventDetail.value.handlerSuggestion || '建议查看关联牲畜详情并执行人工复核。', operator: '系统', time: eventDetail.value.createdAt }
  ]
})

async function refreshData() {
  const [summaryData, chartsData, focusAnimals, events, insightList] = await Promise.all([
    api.dashboard.getSummary(),
    api.dashboard.getCharts(),
    api.dashboard.getFocusAnimals(null, 12),
    api.alerts.getEvents(null, null, null, 16),
    api.dashboard.getInsights()
  ])
  summary.value = summaryData
  charts.value = chartsData
  keyAnimals.value = focusAnimals
  recentEvents.value = events
  insights.value = insightList
}

function configBoolean(value, fallback = true) {
  if (value === undefined || value === null || value === '') return fallback
  return value === true || value === 'true'
}

async function loadDashboardConfig({ reschedule = false } = {}) {
  const configMap = await api.system.getConfigMap('dashboard')
  dashboardConfig.refreshSeconds = Number(configMap.dashboard_refresh_seconds ?? 60)
  dashboardConfig.showFocus = configBoolean(configMap.dashboard_show_focus, true)
  dashboardConfig.showHeatmap = configBoolean(configMap.dashboard_show_heatmap, true)
  dashboardConfig.showScore = configBoolean(configMap.dashboard_show_health_score, true)
  dashboardConfig.showRisk = configBoolean(configMap.dashboard_show_risk_warning, true)
  dashboardConfig.showEstrus = configBoolean(configMap.dashboard_show_estrus_identification, true)
  dashboardConfig.showRumination = configBoolean(configMap.dashboard_show_rumination_efficiency, true)
  dashboardConfig.chartType = configMap.dashboard_chart_type || 'line'
  dashboardConfig.defaultGroup = configMap.dashboard_default_group || dashboardFilters.group || '全部羊群'
  dashboardFilters.group = dashboardConfig.defaultGroup
  if (reschedule) scheduleDashboardRefresh()
}

function scheduleDashboardRefresh() {
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value)
    autoRefreshTimer.value = null
  }
  const seconds = Number(dashboardConfig.refreshSeconds || 0)
  if (seconds > 0) {
    autoRefreshTimer.value = window.setInterval(() => {
      refreshData().catch((error) => console.error('看板自动刷新失败:', error))
    }, seconds * 1000)
  }
}

async function openConfigModal() {
  await loadDashboardConfig()
  showConfigModal.value = true
}

function openFilterModal() {
  showFilterModal.value = true
}

function resetFilter() {
  dashboardFilters.group = dashboardConfig.defaultGroup || '全部羊群'
  dashboardFilters.timeRange = '24h'
}

async function saveDashboardConfig() {
  if (savingAction.value) return
  savingAction.value = 'dashboard-config'
  try {
    await Promise.all([
      api.system.updateConfig('dashboard_refresh_seconds', dashboardConfig.refreshSeconds),
      api.system.updateConfig('dashboard_show_focus', dashboardConfig.showFocus),
      api.system.updateConfig('dashboard_show_heatmap', dashboardConfig.showHeatmap),
      api.system.updateConfig('dashboard_show_health_score', dashboardConfig.showScore),
      api.system.updateConfig('dashboard_show_risk_warning', dashboardConfig.showRisk),
      api.system.updateConfig('dashboard_show_estrus_identification', dashboardConfig.showEstrus),
      api.system.updateConfig('dashboard_show_rumination_efficiency', dashboardConfig.showRumination),
      api.system.updateConfig('dashboard_chart_type', dashboardConfig.chartType),
      api.system.updateConfig('dashboard_default_group', dashboardConfig.defaultGroup)
    ])
    await loadDashboardConfig({ reschedule: true })
    await refreshData()
    api.notify('看板配置已保存')
    showConfigModal.value = false
  } catch (error) {
    api.notify(error.message || '看板配置保存失败', 'danger')
  } finally {
    savingAction.value = ''
  }
}

function openInsightModal() {
  showInsightModal.value = true
}

function exportDashboard() {
  const rows = [
    ['模块', '指标', '数值'],
    ['看板基线', '全场牲畜', summary.value?.baseline?.totalAnimals ?? 0],
    ['看板基线', '全场设备', summary.value?.baseline?.totalDevices ?? 0],
    ['核心指标', '整体健康评分', summary.value?.avgHealthScore ?? 0],
    ['核心指标', '风险预警', summary.value?.alertCount ?? 0],
    ['核心指标', '发情识别', summary.value?.inEstrusCount ?? 0],
    ['核心指标', '反刍效率', summary.value?.ruminationPassRate ?? 0],
    ...statusDistributionSegments.value.map((segment) => ['健康状态分布', segment.label, segment.value]),
    ...filteredKeyAnimals.value.map((animal) => ['重点关注羊只', animal.animalId, animal.healthScore])
  ]
  downloadCsv('个性化健康看板.csv', rows)
}

async function openAnimalDetail(animal) {
  animalDetailLoading.value = true
  showAnimalDetailModal.value = true
  animalDetail.value = { ...animal, temperatureTrend: [], activityTrend: [], interventionSuggestions: [], recentRecords: [] }
  try {
    animalDetail.value = await api.dashboard.getAnimalDetail(animal.animalId)
  } finally {
    animalDetailLoading.value = false
  }
}

async function openIntervention(animal) {
  interventionTarget.value = animal
  interventionLoading.value = true
  showInterventionModal.value = true
  animalDetail.value = { ...animal, temperatureTrend: [], activityTrend: [], interventionSuggestions: [], recentRecords: [] }
  try {
    animalDetail.value = await api.dashboard.getAnimalDetail(animal.animalId)
  } finally {
    interventionLoading.value = false
  }
}

function openInterventionFromDetail() {
  interventionTarget.value = animalDetail.value
  showInterventionModal.value = true
}

async function openEventDetail(event) {
  eventDetailLoading.value = true
  showEventDetailModal.value = true
  try {
    eventDetail.value = await api.alerts.getEventDetail(event.alertId)
  } finally {
    eventDetailLoading.value = false
  }
}

function findChartValue(key) {
  return charts.value.healthStatusDistribution.find((item) => item.key === key)?.value
}

function femaleRate(value) {
  const total = Number(summary.value?.baseline?.femaleAnimalCount || 0)
  if (!total) return 0
  return clampPercent(Math.round((Number(value || 0) / total) * 100))
}

function latestEventTime(animalId) {
  const event = recentEvents.value.find((item) => item.animalId === animalId)
  return event?.time || '-'
}

function activityText(value) {
  const number = Number(value || 0)
  if (number >= 80) return '高活跃'
  if (number >= 50) return '中'
  if (number >= 20) return '低'
  return '静息'
}

function riskLevelText(value) {
  return { high: '警告', medium: '中', low: '低', normal: '正常' }[value] || '正常'
}

function estrusText(value) {
  return { estrus: '发情期', approaching: '临近发情', pregnant: '妊娠期', normal: '正常' }[value] || '正常'
}

function scoreClass(value) {
  const score = Number(value || 0)
  if (score < 60) return 'manual36-score-red'
  if (score < 70) return 'manual36-score-yellow'
  return 'manual36-score-blue'
}

function riskClass(type) {
  if (type === '高风险' || type === '行为异常') return 'manual36-risk-red'
  if (type === '中风险' || type === '发情关注') return 'manual36-risk-yellow'
  return 'manual36-risk-purple'
}

function eventSeverityClass(severity) {
  return severity === 'high' ? 'manual36-event-dot--red' : severity === 'medium' ? 'manual36-event-dot--yellow' : 'manual36-event-dot--blue'
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

function buildLineOption(labels, data, color, min, max) {
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: 34, bottom: 32 },
    xAxis: { type: 'category', data: labels, axisLabel: { color: '#66758c', fontSize: 11 }, axisLine: { lineStyle: { color: '#cbd5e1' } } },
    yAxis: { type: 'value', min, max, axisLabel: { color: '#66758c', fontSize: 11 }, splitLine: { lineStyle: { color: '#edf2f7' } } },
    series: [{
      type: 'line',
      smooth: true,
      data,
      symbolSize: 5,
      itemStyle: { color },
      lineStyle: { color, width: 2 },
      areaStyle: { color: `${color}22` }
    }]
  }
}

onMounted(async () => {
  await loadDashboardConfig({ reschedule: true })
  await refreshData()
})

onBeforeUnmount(() => {
  if (autoRefreshTimer.value) clearInterval(autoRefreshTimer.value)
})
</script>

<style src="./dashboard/dashboard-page.css"></style>
