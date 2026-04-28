<template>
  <section class="manual-stat-card">
    <div class="manual-stat-head">
      <p class="manual-stat-title">{{ title }}</p>
      <slot name="tag" />
    </div>
    <div class="manual-stat-value">
      <slot name="value">{{ value }}</slot>
    </div>
    <slot />
    <div v-if="progress !== null" class="manual-progress">
      <span :style="{ width: `${safeProgress}%`, background: color }" />
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  value: { type: [String, Number], default: '' },
  progress: { type: [Number, String], default: null },
  color: { type: String, default: 'var(--manual-primary)' }
})

const safeProgress = computed(() => Math.max(0, Math.min(100, Number(props.progress || 0))))
</script>
