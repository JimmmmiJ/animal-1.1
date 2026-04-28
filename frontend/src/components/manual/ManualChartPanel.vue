<template>
  <section class="manual-panel">
    <header class="manual-panel-header">
      <h2 class="manual-panel-title">{{ title }}</h2>
      <slot name="actions" />
    </header>
    <div class="manual-panel-body">
      <slot />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, required: true }
})
</script>
