<template>
<div class="manual38-overlay">
      <section class="manual38-modal manual38-modal--confirm">
        <button class="manual38-close" @click="$emit('close')">×</button>
        <h3>{{ confirmDialog.title }}</h3>
        <p class="manual38-confirm-text">{{ confirmDialog.message }}</p>
        <div class="manual38-footer">
          <button class="manual38-btn" @click="$emit('close')">取消</button>
          <button :class="['manual38-btn', confirmDialog.tone === 'danger' ? 'manual38-btn--red' : 'manual38-btn--primary']" @click="$emit('confirm')">
            {{ confirmDialog.confirmLabel }}
          </button>
        </div>
      </section>
    </div>
</template>

<script setup>
defineProps({
  confirmDialog: { type: Object, required: true }
})

defineEmits(['close', 'confirm'])
</script>
