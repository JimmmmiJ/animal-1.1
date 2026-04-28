<template>
  <button :class="classes" :type="type" :disabled="disabled">
    <slot />
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: { type: String, default: 'button' },
  tone: { type: String, default: 'default' },
  size: { type: String, default: 'default' },
  disabled: { type: Boolean, default: false }
})

const classes = computed(() => [
  'manual-btn',
  props.tone !== 'default' && `manual-btn--${props.tone}`,
  props.size !== 'default' && `manual-btn--${props.size}`
])
</script>
