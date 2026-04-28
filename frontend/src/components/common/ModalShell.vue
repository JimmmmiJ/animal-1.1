<template>
  <Teleport to="body">
    <div
      v-if="modelValue"
      class="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/42 p-4"
      @click.self="handleClose"
    >
      <div :class="['flex max-h-[90vh] w-full flex-col overflow-hidden rounded-[6px] border border-[var(--manual-border)] bg-white shadow-[0_14px_32px_rgba(32,52,90,0.14)]', widthClass]">
        <header class="border-b border-[var(--manual-border)] bg-white px-4 py-3">
          <div class="flex items-center justify-between gap-4">
            <div>
              <h3 class="text-[15px] font-semibold text-[var(--manual-text)]">{{ title }}</h3>
            </div>
            <button
              type="button"
              class="flex h-6 w-6 items-center justify-center rounded-[3px] border border-[var(--manual-border-strong)] bg-white text-[16px] leading-none text-[var(--manual-text-muted)] transition hover:border-[var(--manual-primary)] hover:text-[var(--manual-primary)]"
              @click="handleClose"
            >
              <span class="sr-only">关闭</span>
              ×
            </button>
          </div>
        </header>

        <div class="flex-1 overflow-y-auto px-4 py-3">
          <slot />
        </div>

        <footer v-if="$slots.footer" class="border-t border-[var(--manual-border)] bg-[#fbfcfe] px-4 py-3">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  eyebrow: {
    type: String,
    default: ''
  },
  width: {
    type: String,
    default: '5xl'
  }
})

const emit = defineEmits(['close', 'update:modelValue'])

const widthClass = computed(() => ({
  '3xl': 'max-w-3xl',
  '4xl': 'max-w-4xl',
  '5xl': 'max-w-5xl',
  '6xl': 'max-w-6xl',
  '7xl': 'max-w-7xl'
}[props.width] || 'max-w-5xl'))

function handleClose() {
  emit('update:modelValue', false)
  emit('close')
}
</script>
