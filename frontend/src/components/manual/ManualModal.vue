<template>
  <div class="manual-modal-mask" @click.self="$emit('close')">
    <section :class="['manual-modal-shell', widthClass]">
      <button class="manual-modal-close" type="button" @click="$emit('close')">×</button>
      <h3 class="manual-modal-title">{{ title }}</h3>
      <div class="manual-modal-body">
        <slot />
      </div>
      <footer v-if="$slots.footer" class="manual-modal-footer">
        <slot name="footer" />
      </footer>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  width: { type: String, default: 'md' }
})

defineEmits(['close'])

const widthClass = computed(() => `manual-modal-shell--${props.width}`)
</script>
