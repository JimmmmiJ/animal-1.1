<template>
  <div class="manual35-mask" @click.self="$emit('close')">
    <section class="manual35-modal manual35-modal--diet">
      <button class="manual35-close" @click="$emit('close')">×</button>
      <h3>{{ form.animalId ? `调整 ${formatAnimalId(form.animalId)} 饲喂方案` : '调整饲喂方案' }}</h3>
      <div class="manual35-form-grid">
        <label class="manual35-field">
          <span>饲草类型</span>
          <select v-model="form.strategy">
            <option value="苜蓿">苜蓿</option>
            <option value="青贮">青贮</option>
            <option value="高纤维">高纤维</option>
          </select>
        </label>
        <label class="manual35-field">
          <span>精料配比</span>
          <select v-model="form.concentrateRatio">
            <option value="15%">15%</option>
            <option value="20%">20%</option>
            <option value="25%">25%</option>
          </select>
        </label>
        <label class="manual35-field">
          <span>日饲喂量(kg)</span>
          <input v-model="form.dailyFeedAmount" />
        </label>
        <label class="manual35-field">
          <span>饲喂频次</span>
          <select v-model="form.frequency">
            <option value="2次/天">2次/天</option>
            <option value="3次/天">3次/天</option>
            <option value="4次/天">4次/天</option>
          </select>
        </label>
        <div class="manual35-check-group">
          <span>特殊添加剂</span>
          <label><input v-model="form.additives" type="checkbox" value="益生菌" /> 益生菌</label>
          <label><input v-model="form.additives" type="checkbox" value="复合维生素" /> 复合维生素</label>
          <label><input v-model="form.additives" type="checkbox" value="矿物质补充剂" /> 矿物质补充剂</label>
        </div>
        <label class="manual35-field manual35-field--full">
          <span>调整原因</span>
          <textarea v-model="form.notes" rows="3"></textarea>
        </label>
      </div>
      <div class="manual35-footer">
        <button class="manual35-btn" @click="$emit('close')">取消</button>
        <button class="manual35-btn manual35-btn--primary" :disabled="saving" @click="$emit('save')">
          {{ saving ? '保存中...' : '保存方案' }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { formatAnimalId } from '@/utils/formatters'

defineProps({
  form: { type: Object, required: true },
  saving: { type: Boolean, default: false }
})

defineEmits(['close', 'save'])
</script>
