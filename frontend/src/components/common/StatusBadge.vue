<template>
  <span :class="['inline-flex min-w-[34px] items-center justify-center rounded-[4px] border px-2 py-[1px] text-[11px] font-medium leading-5', toneClass]">
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: {
    type: String,
    default: ''
  },
  tone: {
    type: String,
    default: 'gray'
  }
})

const toneClass = computed(() => ({
  green: 'border-emerald-200 bg-[var(--manual-success-soft)] text-[var(--manual-success)]',
  blue: 'border-sky-200 bg-[var(--manual-info-soft)] text-[var(--manual-info)]',
  yellow: 'border-amber-200 bg-[var(--manual-warning-soft)] text-[var(--manual-warning)]',
  orange: 'border-orange-200 bg-orange-50 text-orange-600',
  pink: 'border-pink-200 bg-pink-50 text-pink-600',
  red: 'border-rose-200 bg-[var(--manual-danger-soft)] text-[var(--manual-danger)]',
  purple: 'border-fuchsia-200 bg-[var(--manual-purple-soft)] text-[var(--manual-purple)]',
  slate: 'border-slate-200 bg-slate-50 text-slate-600',
  gray: 'border-slate-200 bg-slate-50 text-slate-600'
}[props.tone] || 'border-slate-200 bg-slate-50 text-slate-600'))
</script>
