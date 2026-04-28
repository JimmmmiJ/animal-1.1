<template>
  <section class="manual34-panel">
    <div class="manual34-panel-head">
      <h2>风险牲畜列表</h2>
      <div class="manual34-panel-actions">
        <button class="manual34-mini-btn" @click="$emit('filter')">筛选</button>
        <button class="manual34-mini-btn" @click="$emit('export')">导出</button>
      </div>
    </div>
    <div class="manual34-table-wrap">
      <table class="manual34-table">
        <thead>
          <tr>
            <th>牲畜编号</th>
            <th>风险等级</th>
            <th>风险分数</th>
            <th>风险类型</th>
            <th>主要症状</th>
            <th>风险趋势</th>
            <th>最近更新</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="animal in animals" :key="animal.animalId">
            <td class="manual34-strong">{{ formatAnimalId(animal.animalId) }}</td>
            <td>
              <span class="manual34-risk-dot" :class="`manual34-risk-dot--${configuredRiskLevel(animal)}`"></span>
              {{ riskShortText(configuredRiskLevel(animal)) }}
            </td>
            <td>{{ animal.riskScore }}</td>
            <td>{{ animal.mainRisk }}</td>
            <td class="manual34-symptoms">{{ animal.mainSymptoms }}</td>
            <td :class="riskTrendClass(configuredRiskLevel(animal))">{{ riskTrendText(configuredRiskLevel(animal)) }}</td>
            <td>{{ animal.lastCheckTimeText }}</td>
            <td>
              <div class="manual34-row-actions">
                <button class="manual34-link manual34-link--blue" @click="$emit('detail', animal)">详情</button>
                <button class="manual34-link manual34-link--green" @click="$emit('medical-record', animal)">病历</button>
                <button class="manual34-link manual34-link--orange" @click="$emit('treatment', animal)">治疗</button>
              </div>
            </td>
          </tr>
          <tr v-if="!animals.length">
            <td colspan="8" class="manual34-empty">暂无风险牲畜</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { formatAnimalId } from '@/utils/formatters'

defineProps({
  animals: { type: Array, default: () => [] },
  configuredRiskLevel: { type: Function, required: true },
  riskShortText: { type: Function, required: true },
  riskTrendText: { type: Function, required: true },
  riskTrendClass: { type: Function, required: true }
})

defineEmits(['filter', 'export', 'detail', 'medical-record', 'treatment'])
</script>
