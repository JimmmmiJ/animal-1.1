<template>
  <div :class="gridClass">
    <div
      v-for="item in items"
      :key="item.key || item.label"
      class="rounded-[5px] border border-[var(--manual-border)] bg-white px-3 py-2"
    >
      <p class="text-[11px] font-medium text-[var(--manual-text-muted)]">{{ item.label }}</p>
      <p :class="['mt-1 text-[12px] text-[var(--manual-text)]', item.emphasis ? 'text-[14px] font-semibold' : '']">
        {{ item.value ?? '-' }}
      </p>
      <p v-if="item.helper" class="mt-1 text-[11px] text-[var(--manual-text-muted)]">{{ item.helper }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  },
  columns: {
    type: Number,
    default: 3
  }
})

const gridClass = computed(() => {
  if (props.columns === 2) return 'grid gap-2.5 md:grid-cols-2'
  if (props.columns === 4) return 'grid gap-2.5 md:grid-cols-2 xl:grid-cols-4'
  return 'grid gap-2.5 md:grid-cols-2 xl:grid-cols-3'
})
</script>
