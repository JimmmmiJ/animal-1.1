<template>
  <section class="manual35-panel">
    <div class="manual35-panel-head">
      <h2>羊只行为分析详情</h2>
      <div class="manual35-panel-actions">
        <button class="manual35-mini-btn" @click="$emit('filter')">筛选</button>
        <button class="manual35-mini-btn" @click="$emit('export')">导出</button>
      </div>
    </div>
    <div class="manual35-table-wrap">
      <table class="manual35-table">
        <thead>
          <tr>
            <th>牲畜编号</th>
            <th>反刍时间(分钟)</th>
            <th>采食次数</th>
            <th>反刍效率</th>
            <th>采食质量</th>
            <th>行为状态</th>
            <th>最后更新</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="sheep in sheepList" :key="sheep.animalId">
            <td class="manual35-strong">{{ formatAnimalId(sheep.animalId) }}</td>
            <td>{{ sheep.ruminationTime }}</td>
            <td>{{ sheep.feedingCount }}</td>
            <td>
              <span class="manual35-inline-progress manual35-inline-progress--blue"><i :style="{ width: `${clampPercent(sheep.ruminationEfficiency)}%` }"></i></span>
              {{ sheep.ruminationEfficiency }}%
            </td>
            <td>
              <span class="manual35-inline-progress manual35-inline-progress--green"><i :style="{ width: `${clampPercent(sheep.feedingQuality)}%` }"></i></span>
              {{ sheep.feedingQuality }}%
            </td>
            <td>
              <span class="manual35-status-dot" :class="`manual35-status-dot--${configuredBehaviorStatus(sheep)}`"></span>
              {{ behaviorStatusShortText(configuredBehaviorStatus(sheep)) }}
            </td>
            <td>{{ sheep.lastUpdateText }}</td>
            <td>
              <div class="manual35-row-actions">
                <button class="manual35-link manual35-link--blue" @click="$emit('detail', sheep)">详情</button>
                <button class="manual35-link manual35-link--green" @click="$emit('diet', sheep)">调整饲喂</button>
              </div>
            </td>
          </tr>
          <tr v-if="!sheepList.length">
            <td colspan="8" class="manual35-empty">暂无行为数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { formatAnimalId } from '@/utils/formatters'

defineProps({
  sheepList: { type: Array, default: () => [] },
  configuredBehaviorStatus: { type: Function, required: true },
  behaviorStatusShortText: { type: Function, required: true },
  clampPercent: { type: Function, required: true }
})

defineEmits(['filter', 'export', 'detail', 'diet'])
</script>
