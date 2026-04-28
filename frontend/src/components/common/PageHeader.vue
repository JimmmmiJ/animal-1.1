<template>
  <header class="manual-page-header">
    <div>
      <h2 class="manual-page-title">{{ title }}</h2>
      <p v-if="description" class="manual-page-description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="manual-page-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  }
})
</script>
