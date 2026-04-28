<template>
  <article class="manual-stat-card">
    <div class="manual-stat-head">
      <p class="manual-stat-title">{{ title }}</p>
      <slot name="extra" />
    </div>
    <p class="manual-stat-value" :style="{ color: accentColor }">{{ value }}</p>
    <p v-if="helper" class="manual-stat-helper">{{ helper }}</p>
    <div v-if="showProgress" class="manual-progress">
      <span :style="{ width: `${progressValue}%`, backgroundColor: accentColor }" />
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  value: {
    type: [String, Number],
    default: '-'
  },
  helper: {
    type: String,
    default: ''
  },
  progress: {
    type: Number,
    default: null
  },
  tone: {
    type: String,
    default: 'primary'
  }
})

const toneMap = {
  primary: '#2550a6',
  success: '#2ba568',
  warning: '#e0a22a',
  danger: '#e35a5a',
  purple: '#7a68d8',
  info: '#4a90e2'
}

const accentColor = computed(() => toneMap[props.tone] || toneMap.primary)
const showProgress = computed(() => props.progress !== null && props.progress !== undefined)
const progressValue = computed(() => Math.max(0, Math.min(100, Number(props.progress || 0))))
</script>
