<template>
  <section class="manual-panel">
    <header v-if="title || subtitle || $slots.actions" class="manual-panel-header">
      <div>
        <h3 v-if="title" class="manual-panel-title">{{ title }}</h3>
        <p v-if="subtitle" class="manual-panel-subtitle">{{ subtitle }}</p>
      </div>
      <div v-if="$slots.actions" class="manual-filter-bar">
        <slot name="actions" />
      </div>
    </header>
    <div class="manual-panel-body">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  }
})
</script>
