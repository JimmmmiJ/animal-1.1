<template>
  <div class="manual35-mask" @click.self="$emit('close')">
    <section class="manual35-modal manual35-modal--report">
      <button class="manual35-close" @click="$emit('close')">×</button>
      <h3>反刍采食行为分析报告</h3>
      <div class="manual35-report-grid">
        <div class="manual35-report-chart">
          <h4>行为模式分布</h4>
          <ChartBox :option="reportPatternOption" height="250px" />
        </div>
        <div class="manual35-report-chart">
          <h4>群体健康趋势</h4>
          <ChartBox :option="reportHealthOption" height="250px" />
        </div>
      </div>
      <div class="manual35-analysis">
        <h4>分析总结</h4>
        <p>{{ reportSummaryText }}</p>
        <p v-if="reportRecommendationText">{{ reportRecommendationText }}</p>
      </div>
      <div class="manual35-footer">
        <button class="manual35-btn manual35-btn--primary" @click="$emit('close')">关闭</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import ChartBox from '@/components/common/ChartBox.vue'

defineProps({
  reportPatternOption: { type: Object, required: true },
  reportHealthOption: { type: Object, required: true },
  reportSummaryText: { type: String, default: '' },
  reportRecommendationText: { type: String, default: '' }
})

defineEmits(['close'])
</script>
