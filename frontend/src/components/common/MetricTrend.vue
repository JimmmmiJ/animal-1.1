<template>
  <div class="rounded-lg border border-[var(--manual-border)] bg-white p-4">
    <div class="mb-4 flex items-center justify-between">
      <h5 class="text-sm font-semibold text-[var(--manual-text)]">{{ title }}</h5>
      <span v-if="unit" class="text-xs text-[var(--manual-text-muted)]">{{ unit }}</span>
    </div>

    <div v-if="points.length" class="space-y-3">
      <div v-for="point in points" :key="point.label" class="grid grid-cols-[68px_1fr_auto] items-center gap-3">
        <span class="text-xs text-[var(--manual-text-muted)]">{{ point.label }}</span>
        <div class="h-2 rounded-full bg-[#eef3fa]">
          <div class="h-2 rounded-full" :style="{ width: `${resolveWidth(point.value)}%`, backgroundColor: color }" />
        </div>
        <span class="text-xs font-medium text-[var(--manual-text)]">{{ formatValue(point.value) }}</span>
      </div>
    </div>

    <div v-else class="py-8 text-center text-sm text-[var(--manual-text-muted)]">暂无数据</div>
  </div>
</template>

<script setup>
const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  points: {
    type: Array,
    default: () => []
  },
  unit: {
    type: String,
    default: ''
  },
  color: {
    type: String,
    default: '#2563eb'
  },
  max: {
    type: Number,
    default: 100
  }
})

function resolveWidth(value) {
  const numeric = Number(value || 0)
  const ratio = props.max > 0 ? numeric / props.max : 0
  return Math.max(0, Math.min(100, Math.round(ratio * 100)))
}

function formatValue(value) {
  if (value === null || value === undefined || value === '') return '-'
  return props.unit ? `${value}${props.unit}` : value
}
</script>
