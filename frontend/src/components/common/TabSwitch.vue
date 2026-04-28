<template>
  <div class="manual-tabs">
    <button
      v-for="item in items"
      :key="item.value"
      type="button"
      class="manual-tab"
      :class="{ 'is-active': modelValue === item.value }"
      @click="$emit('update:modelValue', item.value)"
    >
      {{ item.label }}
    </button>
  </div>
</template>

<script setup>
defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  items: {
    type: Array,
    default: () => []
  }
})

defineEmits(['update:modelValue'])
</script>
