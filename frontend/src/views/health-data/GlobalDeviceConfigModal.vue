<template>
  <div class="manual32-overlay" @click.self="$emit('close')">
    <section class="manual32-modal manual32-modal--config">
      <header class="manual32-modal-header">
        <h3>设备配置</h3>
        <button type="button" @click="$emit('close')">×</button>
      </header>
      <div class="manual32-modal-body">
        <div class="manual32-form-grid">
          <label class="manual32-field">
            <span>通信协议</span>
            <select v-model="config.protocol">
              <option value="LoRa">LoRa</option>
              <option value="NB-IoT">NB-IoT</option>
              <option value="4G">4G</option>
              <option value="MQTT">MQTT</option>
            </select>
          </label>
          <label class="manual32-field">
            <span>数据上传频率</span>
            <select v-model.number="config.uploadInterval">
              <option v-for="item in frequencyOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label class="manual32-field">
            <span>体温报警阈值</span>
            <input v-model.number="config.temperatureMax" type="number" step="0.1" />
          </label>
          <label class="manual32-field">
            <span>电量报警阈值</span>
            <input v-model.number="config.lowBattery" type="number" />
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
  frequencyOptions: {
    type: Array,
    default: () => []
  },
  saving: {
    type: Boolean,
    default: false
  }
})

defineEmits(['close', 'save'])
</script>

<style src="./health-data-modal.css"></style>
