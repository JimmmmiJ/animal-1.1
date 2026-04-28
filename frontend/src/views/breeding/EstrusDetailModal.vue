<template>
  <section class="manual33-modal manual33-modal--detail">
    <header class="manual33-modal-header">
      <h3>{{ selectedAnimalCode }} 详情</h3>
      <button type="button" @click="$emit('close')">×</button>
    </header>
    <div class="manual33-modal-body">
      <div v-if="loading" class="manual-empty">加载详情中...</div>
      <template v-else>
        <div class="manual33-detail-grid">
          <div class="manual33-info-card">
            <h4>基本信息</h4>
            <p><span>牲畜编号:</span><b>{{ selectedAnimalCode }}</b></p>
            <p><span>姓名:</span><b>{{ selectedAnimalCode }}</b></p>
            <p><span>年龄:</span><b>{{ detail.age ? `${detail.age}岁` : '-' }}</b></p>
            <p><span>品种:</span><b>{{ detail.breed || '-' }}</b></p>
            <p><span>性别:</span><b>雌</b></p>
          </div>
          <div class="manual33-info-card">
            <h4>发情期信息</h4>
            <p><span>状态:</span><b class="manual33-text-pink">{{ statusText(detail) }}</b></p>
            <p><span>发情概率:</span><b>{{ probability(detail) }}%</b></p>
            <p><span>发情阶段:</span><b>{{ stageText(detail) }}</b></p>
            <p><span>活动量指数:</span><b>{{ detail.activityIndex ?? '-' }}</b></p>
            <p><span>预计结束:</span><b>{{ endTimeText(detail) }}</b></p>
          </div>
        </div>
        <div class="manual33-section-title">近期活动模式</div>
        <table class="manual33-inner-table">
          <thead>
            <tr>
              <th>时间</th>
              <th>活动量</th>
              <th>步数</th>
              <th>距离(M)</th>
              <th>发情概率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in activityRows" :key="row.time">
              <td>{{ row.time }}</td>
              <td>{{ row.activity }}</td>
              <td>{{ row.steps }}</td>
              <td>{{ row.distance }}</td>
              <td>{{ row.probability }}%</td>
            </tr>
            <tr v-if="!activityRows.length">
              <td colspan="5" class="manual-empty">暂无活动模式记录</td>
            </tr>
          </tbody>
        </table>
      </template>
    </div>
    <footer class="manual33-modal-footer">
      <button class="manual33-btn" @click="$emit('close')">关闭</button>
      <button class="manual33-btn manual33-btn--purple" :disabled="!detail.animalId" @click="$emit('plan', detail)">配种计划</button>
    </footer>
  </section>
</template>

<script setup>
defineProps({
  detail: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
  selectedAnimalCode: { type: String, default: '-' },
  activityRows: { type: Array, default: () => [] },
  statusText: { type: Function, required: true },
  probability: { type: Function, required: true },
  stageText: { type: Function, required: true },
  endTimeText: { type: Function, required: true }
})

defineEmits(['close', 'plan'])
</script>
