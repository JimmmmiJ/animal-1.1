<template>
  <div :class="['manual-table-wrap', scroll && 'manual-table-scroll']">
    <table class="manual-table">
      <slot />
    </table>
  </div>
</template>

<script setup>
defineProps({
  scroll: { type: Boolean, default: false }
})
</script>
