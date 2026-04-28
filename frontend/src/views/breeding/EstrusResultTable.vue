<template>
  <PanelCard title="发情期检测结果" class="manual33-panel">
    <template #actions>
      <button class="manual33-mini-btn" @click="$emit('filter')">筛选</button>
      <button class="manual33-mini-btn" @click="$emit('export')">导出</button>
    </template>

    <div class="manual-table-wrap manual-table-scroll manual33-table-wrap">
      <table class="manual-table manual33-table">
        <thead>
          <tr>
            <th>牲畜编号</th>
            <th>姓名</th>
            <th>状态</th>
            <th>发情概率</th>
            <th>发情阶段</th>
            <th>开始时间</th>
            <th>预计结束</th>
            <th>活动量指数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="animal in animals" :key="animal.animalId">
            <td class="manual33-strong">{{ displayAnimalCode(animal) }}</td>
            <td>{{ displayAnimalCode(animal) }}</td>
            <td>
              <span class="manual33-status">
                <i :class="`manual33-dot manual33-dot--${statusTone(animal)}`"></i>
                {{ statusText(animal) }}
              </span>
            </td>
            <td>
              <span class="manual33-prob">
                <i><em :style="{ width: `${probability(animal)}%` }"></em></i>
                {{ probability(animal) }}%
              </span>
            </td>
            <td>{{ stageText(animal) }}</td>
            <td>{{ startTimeText(animal) }}</td>
            <td>{{ endTimeText(animal) }}</td>
            <td>{{ animal.activityIndex ?? '-' }}</td>
            <td>
              <div class="manual33-actions">
                <button class="manual33-link manual33-link--blue" @click="$emit('detail', animal)">详情</button>
                <button class="manual33-link manual33-link--purple" @click="$emit('plan', animal)">配种计划</button>
                <button class="manual33-link manual33-link--green" @click="$emit('history', animal)">历史</button>
              </div>
            </td>
          </tr>
          <tr v-if="!animals.length">
            <td colspan="9" class="manual-empty">暂无发情期检测结果</td>
          </tr>
        </tbody>
      </table>
    </div>
  </PanelCard>
</template>

<script setup>
import PanelCard from '@/components/common/PanelCard.vue'

defineProps({
  animals: { type: Array, default: () => [] },
  displayAnimalCode: { type: Function, required: true },
  statusTone: { type: Function, required: true },
  statusText: { type: Function, required: true },
  probability: { type: Function, required: true },
  stageText: { type: Function, required: true },
  startTimeText: { type: Function, required: true },
  endTimeText: { type: Function, required: true }
})

defineEmits(['filter', 'export', 'detail', 'plan', 'history'])
</script>
