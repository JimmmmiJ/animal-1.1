<template>
  <div class="manual34-mask" @click.self="$emit('close')">
    <section class="manual34-modal manual34-modal--assessment">
      <button class="manual34-close" @click="$emit('close')">×</button>
      <h3>疾病风险评估</h3>
      <div class="manual34-form-grid">
        <label class="manual34-field">
          <span>牲畜编号</span>
          <input :value="animalId" list="manual34-animal-list" placeholder="输入牲畜编号" @input="$emit('update:animalId', $event.target.value)" />
          <datalist id="manual34-animal-list">
            <option v-for="animal in animals" :key="animal.animalId" :value="animal.animalId">{{ formatAnimalId(animal.animalId) }}</option>
          </datalist>
        </label>
        <label class="manual34-field">
          <span>评估时间</span>
          <input v-model="form.assessmentTime" type="datetime-local" />
        </label>
        <label class="manual34-field manual34-field--full">
          <span>症状描述</span>
          <textarea v-model="form.symptomDescription" rows="3" placeholder="详细描述观察到的症状..."></textarea>
        </label>
        <p class="manual34-example">例如：体温升高、食欲减退、活动量下降等</p>
        <label class="manual34-field">
          <span>体温 (℃)</span>
          <input v-model.number="form.temperature" type="number" step="0.1" placeholder="38.5" />
        </label>
        <label class="manual34-field">
          <span>心率 (bpm)</span>
          <input v-model.number="form.heartRate" type="number" placeholder="80" />
        </label>
        <label class="manual34-field manual34-field--full">
          <span>初步诊断</span>
          <select v-model="form.initialDiagnosis">
            <option value="">请选择诊断结果</option>
            <option value="发热">发热</option>
            <option value="呼吸道感染">呼吸道感染</option>
            <option value="消化系统问题">消化系统问题</option>
            <option value="关节炎症">关节炎症</option>
          </select>
        </label>
        <label class="manual34-field manual34-field--full">
          <span>备注</span>
          <textarea v-model="form.notes" rows="3" placeholder="补充说明..."></textarea>
        </label>
      </div>
      <div class="manual34-footer">
        <button class="manual34-btn" @click="$emit('close')">取消</button>
        <button class="manual34-btn manual34-btn--primary" @click="$emit('submit')">提交评估</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { formatAnimalId } from '@/utils/formatters'

defineProps({
  animals: { type: Array, default: () => [] },
  animalId: { type: String, default: '' },
  form: { type: Object, required: true }
})

defineEmits(['close', 'submit', 'update:animalId'])
</script>
