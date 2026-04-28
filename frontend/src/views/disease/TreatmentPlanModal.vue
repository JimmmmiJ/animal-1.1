<template>
  <div class="manual34-mask" @click.self="$emit('close')">
    <section class="manual34-modal manual34-modal--treatment">
      <button class="manual34-close" @click="$emit('close')">×</button>
      <h3>{{ form.animalId ? `${formatAnimalId(form.animalId)} 治疗计划` : '治疗计划' }}</h3>
      <div class="manual34-treatment-form">
        <label class="manual34-field manual34-field--full">
          <span>治疗方案</span>
          <select v-model="form.diagnosis">
            <option value="抗生素治疗">抗生素治疗</option>
            <option value="消化调理">消化调理</option>
            <option value="退热护理">退热护理</option>
            <option value="关节护理">关节护理</option>
          </select>
        </label>
        <label class="manual34-field manual34-field--full">
          <span>用药方案</span>
          <textarea v-model="form.treatmentPlan" rows="2" placeholder="针对发热的治疗方案..."></textarea>
        </label>
        <div class="manual34-period">
          <label class="manual34-field">
            <span>治疗周期</span>
            <input v-model="form.startDate" type="date" />
          </label>
          <span>至</span>
          <label class="manual34-field manual34-field--no-label">
            <input v-model="form.endDate" type="date" />
          </label>
        </div>
        <label class="manual34-field manual34-field--full">
          <span>护理要求</span>
          <textarea v-model="form.careRequirement" rows="2" placeholder="特殊护理要求..."></textarea>
        </label>
        <label class="manual34-field manual34-field--full">
          <span>复查安排</span>
          <input v-model="form.recheckAt" type="datetime-local" />
        </label>
        <label class="manual34-field manual34-field--full">
          <span>备注</span>
          <textarea v-model="form.notes" rows="2" placeholder="补充说明..."></textarea>
        </label>
      </div>
      <div class="manual34-footer">
        <button class="manual34-btn" @click="$emit('close')">取消</button>
        <button class="manual34-btn manual34-btn--primary" :disabled="saving" @click="$emit('save')">
          {{ saving ? '保存中...' : '保存计划' }}
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
