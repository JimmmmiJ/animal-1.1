<template>
<div class="manual36-mask" @click.self="$emit('close')">
      <section class="manual36-modal manual36-modal--insight">
        <button class="manual36-close" @click="$emit('close')">×</button>
        <h3>健康洞察分析</h3>
        <div class="manual36-insight-grid">
          <div class="manual36-soft-card">
            <h4>群体健康趋势</h4>
            <ChartBox :option="groupHealthTrendOption" height="210px" />
          </div>
          <div class="manual36-soft-card">
            <h4>风险因素关联</h4>
            <ChartBox :option="riskCorrelationOption" height="210px" />
          </div>
        </div>
        <div class="manual36-analysis">
          <h4>智能分析结论</h4>
          <p v-for="item in insightRows" :key="item.no">
            <b>{{ item.no }}</b>
            <span>{{ item.text }}</span>
          </p>
        </div>
        <div class="manual36-footer">
          <button class="manual36-btn manual36-btn--primary" @click="$emit('close')">关闭</button>
        </div>
      </section>
    </div>
</template>

<script setup>
import ChartBox from '@/components/common/ChartBox.vue'

defineProps({
  groupHealthTrendOption: { type: Object, required: true },
  riskCorrelationOption: { type: Object, required: true },
  insightRows: { type: Array, default: () => [] }
})

defineEmits(['close'])
</script>
