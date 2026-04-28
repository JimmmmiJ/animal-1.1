<template>
  <div class="manual32-overlay" @click.self="$emit('close')">
    <section class="manual32-modal manual32-modal--config">
      <header class="manual32-modal-header">
        <h3>{{ config.deviceId ? `配置设备 ${config.deviceId}` : '配置设备' }}</h3>
        <button type="button" @click="$emit('close')">×</button>
      </header>
      <div class="manual32-modal-body">
        <div class="manual32-form-grid">
          <label class="manual32-field">
            <span>设备ID</span>
            <input v-model="config.deviceId" type="text" disabled />
          </label>
          <label class="manual32-field">
            <span>牲畜编号</span>
            <input v-model="config.animalId" type="text" />
          </label>
          <label class="manual32-field">
            <span>体温报警阈值</span>
            <input v-model.number="config.temperatureMaxThreshold" type="number" step="0.1" />
          </label>
          <label class="manual32-field">
            <span>心率报警阈值</span>
            <input v-model.number="config.heartRateMaxThreshold" type="number" />
          </label>
          <label class="manual32-field manual32-field--full">
            <span>健康评估模型</span>
            <select v-model="config.healthModel">
              <option value="基础模型">基础模型</option>
              <option value="增强模型">增强模型</option>
              <option value="智能模型">智能模型</option>
            </select>
          </label>
          <label class="manual32-field manual32-field--full">
            <span>备注说明</span>
            <textarea v-model="config.note" rows="4" />
          </label>
        </div>
      </div>
      <footer class="manual32-modal-footer">
        <button class="manual32-btn" @click="$emit('close')">取消</button>
        <button class="manual32-btn manual32-btn--primary" :disabled="saving" @click="$emit('save')">
          {{ saving ? '保存中...' : '保存配置' }}
        </button>
      </footer>
    </section>
  </div>
</template>

<script setup>
defineProps({
  config: {
    type: Object,
    required: true
  },
  saving: {
    type: Boolean,
    default: false
  }
})

defineEmits(['close', 'save'])
</script>

<style src="./health-data-modal.css"></style>
