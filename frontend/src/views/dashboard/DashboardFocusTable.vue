<template>
<section class="manual36-panel manual36-focus-panel">
        <div class="manual36-panel-head">
          <h2>重点关注羊只</h2>
          <div class="manual36-tag-group">
            <span>高风险</span>
            <span>中风险</span>
          </div>
        </div>
        <div class="manual36-table-wrap">
          <table class="manual36-table">
            <thead>
              <tr>
                <th>牲畜编号</th>
                <th>健康评分</th>
                <th>风险类型</th>
                <th>体温(℃)</th>
                <th>活动量</th>
                <th>最后更新</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="animal in animals" :key="animal.animalId">
                <td class="manual36-strong">{{ formatAnimalId(animal.animalId) }}</td>
                <td><b :class="scoreClass(animal.healthScore)">{{ animal.healthScore }}</b></td>
                <td><span class="manual36-risk-label" :class="riskClass(animal.riskTypeText)">{{ animal.riskTypeText }}</span></td>
                <td>{{ metricValue(animal.temperature) }}</td>
                <td>{{ animal.activityText }}</td>
                <td>{{ latestEventTime(animal.animalId) }}</td>
                <td>
                  <div class="manual36-row-actions">
                    <button class="manual36-link manual36-link--blue" @click="$emit('detail', animal)">详情</button>
                    <button class="manual36-link manual36-link--green" @click="$emit('intervention', animal)">干预</button>
                  </div>
                </td>
              </tr>
              <tr v-if="!animals.length">
                <td colspan="7" class="manual36-empty">暂无重点关注羊只</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
</template>

<script setup>
defineProps({
  animals: { type: Array, default: () => [] },
  formatAnimalId: { type: Function, required: true },
  scoreClass: { type: Function, required: true },
  riskClass: { type: Function, required: true },
  metricValue: { type: Function, required: true },
  latestEventTime: { type: Function, required: true }
})

defineEmits(['detail', 'intervention'])
</script>
