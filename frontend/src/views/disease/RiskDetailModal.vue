<template>
  <div class="manual34-mask" @click.self="$emit('close')">
    <section class="manual34-modal manual34-modal--detail">
      <button class="manual34-close" @click="$emit('close')">×</button>
      <h3>{{ detail.animalId ? `${formatAnimalId(detail.animalId)} 风险详情` : '风险详情' }}</h3>
      <div v-if="loading" class="manual34-empty">加载详情中...</div>
      <template v-else>
        <div class="manual34-info-grid">
          <div class="manual34-info-card">
            <h4>牲畜信息</h4>
            <p><span>牲畜编号:</span><b>{{ formatAnimalId(detail.animalId) }}</b></p>
            <p><span>风险等级:</span><b>{{ riskShortText(configuredRiskLevel(detail)) }}</b></p>
            <p><span>风险分数:</span><b>{{ detail.riskScore }}</b></p>
            <p><span>风险类型:</span><b>{{ detail.mainRisk }}</b></p>
            <p><span>风险趋势:</span><b :class="riskTrendClass(configuredRiskLevel(detail))">{{ riskTrendText(configuredRiskLevel(detail)) }}</b></p>
          </div>
          <div class="manual34-info-card">
            <h4>健康参数</h4>
            <p><span>体温:</span><b>{{ metricValue(detail.currentTemperature, '℃') }}</b></p>
            <p><span>心率:</span><b>{{ metricValue(detail.currentHeartRate, ' bpm') }}</b></p>
            <p><span>活动量:</span><b>{{ detail.activityTrend?.at?.(-1)?.value ?? '-' }}</b></p>
            <p><span>食欲评分:</span><b>{{ detail.healthScore || '-' }}</b></p>
            <p><span>精神状态:</span><b>{{ configuredRiskLevel(detail) === 'high' ? '萎靡' : '一般' }}</b></p>
          </div>
        </div>

        <div class="manual34-symptom-block">
          <h4>主要症状</h4>
          <div class="manual34-tag-row">
            <span v-for="tag in detailTags" :key="tag" class="manual34-symptom-tag">{{ tag }}</span>
          </div>
        </div>

        <div class="manual34-chart-block">
          <h4>健康趋势图表</h4>
          <ChartBox :option="detailHealthTrendOption" height="170px" />
        </div>

        <div class="manual34-footer">
          <button class="manual34-btn" @click="$emit('close')">关闭</button>
          <button class="manual34-btn manual34-btn--orange" @click="$emit('treatment', detail)">制定治疗计划</button>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { formatAnimalId, metricValue } from '@/utils/formatters'
import ChartBox from '@/components/common/ChartBox.vue'

defineProps({
  detail: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
  detailTags: { type: Array, default: () => [] },
  detailHealthTrendOption: { type: Object, required: true },
  configuredRiskLevel: { type: Function, required: true },
  riskShortText: { type: Function, required: true },
  riskTrendText: { type: Function, required: true },
  riskTrendClass: { type: Function, required: true }
})

defineEmits(['close', 'treatment'])
</script>
