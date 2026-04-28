<template>
  <section class="manual33-modal manual33-modal--config">
    <header class="manual33-modal-header">
      <h3>为 {{ selectedAnimalCode }} 制定配种计划</h3>
      <button type="button" @click="$emit('close')">×</button>
    </header>
    <div class="manual33-modal-body">
      <div class="manual33-form-grid">
        <label class="manual33-field">
          <span>推荐公羊</span>
          <select v-model="form.recommendedBull">
            <option>BULL-001</option>
            <option>BULL-002</option>
            <option>BULL-003</option>
          </select>
        </label>
        <label class="manual33-field">
          <span>配种方式</span>
          <select v-model="form.method">
            <option>自然交配</option>
            <option>人工授精</option>
            <option>人工辅助</option>
          </select>
        </label>
        <label class="manual33-field">
          <span>建议配种时间</span>
          <input v-model="form.suggestedTime" type="datetime-local" />
        </label>
        <label class="manual33-field">
          <span>预计受孕概率</span>
          <input v-model.number="form.pregnancyProbability" type="number" min="0" max="100" />
        </label>
        <label class="manual33-field manual33-field--full">
          <span>配种备注</span>
          <textarea v-model="form.notes" rows="4"></textarea>
        </label>
      </div>
    </div>
    <footer class="manual33-modal-footer">
      <button class="manual33-btn" @click="$emit('close')">取消</button>
      <button class="manual33-btn manual33-btn--primary" :disabled="saving" @click="$emit('save')">
        {{ saving ? '保存中...' : '保存计划' }}
      </button>
    </footer>
  </section>
</template>

<script setup>
defineProps({
  form: { type: Object, required: true },
  selectedAnimalCode: { type: String, default: '-' },
  saving: { type: Boolean, default: false }
})

defineEmits(['close', 'save'])
</script>
