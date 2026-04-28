<template>
  <section class="rounded-[6px] border border-[var(--manual-border)] bg-[#fbfcfe] p-3">
    <div class="mb-2.5 flex items-start justify-between gap-3">
      <div>
        <h4 class="text-[13px] font-semibold text-[var(--manual-text)]">{{ title }}</h4>
        <p v-if="subtitle" class="mt-1 text-[11px] text-[var(--manual-text-muted)]">{{ subtitle }}</p>
      </div>
      <slot name="action" />
    </div>
    <slot />
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
