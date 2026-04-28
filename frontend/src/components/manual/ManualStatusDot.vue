<template>
  <span class="manual-status-dot" :style="{ background: color }" />
</template>

<script setup>
defineProps({
  color: { type: String, default: 'var(--manual-success)' }
})
</script>
