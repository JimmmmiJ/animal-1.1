<template>
  <div :class="['manual-form-grid', columns === 2 && 'manual-form-grid--2']">
    <slot />
  </div>
</template>

<script setup>
defineProps({
  columns: { type: Number, default: 2 }
})
</script>
