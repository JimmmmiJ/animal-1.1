<template>
<div class="manual36-mask" @click.self="$emit('close')">
      <section class="manual36-modal manual36-modal--intervention">
        <button class="manual36-close" @click="$emit('close')">×</button>
        <h3>{{ formatAnimalId(target.animalId) }} 健康干预</h3>
        <div v-if="loading" class="manual36-empty">加载建议中...</div>
        <div v-else class="manual36-advice-list">
          <p v-for="item in items" :key="item">{{ item }}</p>
        </div>
        <div class="manual36-footer">
          <button class="manual36-btn manual36-btn--primary" @click="$emit('close')">关闭</button>
        </div>
      </section>
    </div>
</template>

<script setup>
defineProps({
  target: { type: Object, required: true },
  loading: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
  formatAnimalId: { type: Function, required: true }
})

defineEmits(['close'])
</script>
