<template>
  <div class="manual35-mask" @click.self="$emit('close')">
    <section class="manual35-modal manual35-modal--detail">
      <button class="manual35-close" @click="$emit('close')">×</button>
      <h3>{{ detail.animalId ? `${formatAnimalId(detail.animalId)} 行为详情` : '行为详情' }}</h3>
      <div v-if="loading" class="manual35-empty">加载详情中...</div>
      <template v-else>
        <div class="manual35-info-grid">
          <div class="manual35-info-card">
            <h4>基本信息</h4>
            <p><span>牲畜编号:</span><b>{{ formatAnimalId(detail.animalId) }}</b></p>
            <p><span>年龄:</span><b>{{ detail.age ? `${detail.age}岁` : '-' }}</b></p>
            <p><span>品种:</span><b>{{ detail.breed || '-' }}</b></p>
            <p><span>体重:</span><b>{{ detail.weight ? `${detail.weight}kg` : '-' }}</b></p>
            <p><span>健康评分:</span><b>{{ detail.healthScore || '-' }}</b></p>
          </div>
          <div class="manual35-info-card">
            <h4>行为参数</h4>
            <p><span>日均反刍时间:</span><b>{{ detail.ruminationTime }}分钟</b></p>
            <p><span>日均采食次数:</span><b>{{ detail.feedingCount }}次</b></p>
            <p><span>反刍效率:</span><b>{{ detail.ruminationEfficiency }}%</b></p>
            <p><span>采食质量:</span><b>{{ detail.feedingQuality }}%</b></p>
            <p><span>行为状态:</span><b>{{ behaviorStatusShortText(configuredBehaviorStatus(detail)) }}</b></p>
          </div>
        </div>

        <div class="manual35-chart-block">
          <h4>24小时行为模式</h4>
          <ChartBox :option="detailPatternOption" height="160px" />
        </div>

        <div class="manual35-history">
          <h4>历史行为趋势</h4>
          <table class="manual35-table manual35-table--compact">
            <thead>
              <tr>
                <th>日期</th>
                <th>反刍时间</th>
                <th>采食次数</th>
                <th>反刍效率</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in detailHistoryRows" :key="item.date">
                <td>{{ item.date }}</td>
                <td>{{ item.ruminationTime }}分钟</td>
                <td>{{ item.feedingCount }}次</td>
                <td>{{ item.ruminationEfficiency }}%</td>
                <td>
                  <span class="manual35-status-dot" :class="item.status === '正常' ? 'manual35-status-dot--normal' : 'manual35-status-dot--warning'"></span>
                  {{ item.status }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="manual35-footer">
          <button class="manual35-btn" @click="$emit('close')">关闭</button>
          <button class="manual35-btn manual35-btn--green" @click="$emit('diet', detail)">调整饲喂</button>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { formatAnimalId } from '@/utils/formatters'
import ChartBox from '@/components/common/ChartBox.vue'

defineProps({
  detail: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
  detailPatternOption: { type: Object, required: true },
  detailHistoryRows: { type: Array, default: () => [] },
  configuredBehaviorStatus: { type: Function, required: true },
  behaviorStatusShortText: { type: Function, required: true }
})

defineEmits(['close', 'diet'])
</script>
