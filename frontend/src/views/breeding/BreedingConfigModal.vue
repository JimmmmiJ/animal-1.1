<template>
  <section class="manual33-modal manual33-modal--config">
    <header class="manual33-modal-header">
      <h3>配种配置管理</h3>
      <button type="button" @click="$emit('close')">×</button>
    </header>
    <div class="manual33-modal-body">
      <div class="manual33-form-grid">
        <label class="manual33-field">
          <span>发情识别算法</span>
          <input v-model="config.algorithm" readonly />
        </label>
        <label class="manual33-field">
          <span>发情概率阈值</span>
          <input v-model.number="config.probabilityThreshold" type="number" min="0" max="100" />
        </label>
        <label class="manual33-field">
          <span>预警提前天数</span>
          <input v-model.number="config.noticeDays" type="number" min="0" max="30" />
        </label>
        <label class="manual33-field">
          <span>数据采样频率</span>
          <select v-model="config.samplingFrequency">
            <option>高频(每分钟)</option>
            <option>标准(每5分钟)</option>
            <option>低频(每小时)</option>
          </select>
        </label>
        <label class="manual33-field manual33-field--full">
          <span>配种建议策略</span>
          <select v-model="config.strategy">
            <option>自动推荐</option>
            <option>人工复核</option>
            <option>仅预警</option>
          </select>
        </label>
        <label class="manual33-field manual33-field--full">
          <span>特殊备注</span>
          <textarea v-model="config.note" rows="4"></textarea>
        </label>
      </div>
    </div>
    <footer class="manual33-modal-footer">
      <button class="manual33-btn" @click="$emit('close')">取消</button>
      <button class="manual33-btn manual33-btn--primary" :disabled="saving" @click="$emit('save')">
        {{ saving ? '保存中...' : '保存配置' }}
      </button>
    </footer>
  </section>
</template>

<script setup>
defineProps({
  config: { type: Object, required: true },
  saving: { type: Boolean, default: false }
})

defineEmits(['close', 'save'])
</script>
