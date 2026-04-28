<template>
<div class="manual36-mask" @click.self="$emit('close')">
      <section class="manual36-modal manual36-modal--event">
        <button class="manual36-close" @click="$emit('close')">×</button>
        <h3>{{ eventDetail.alertId ? `${eventDetail.alertId} 事件详情` : '近期健康事件' }}</h3>
        <div v-if="loading" class="manual36-empty">加载详情中...</div>
        <template v-else>
          <div class="manual36-detail-grid manual36-detail-grid--event">
            <div class="manual36-info-card">
              <h4>事件信息</h4>
              <p><span>事件类型:</span><b>{{ eventDetail.typeText }}</b></p>
              <p><span>牲畜编号:</span><b>{{ formatAnimalId(eventDetail.animalId) }}</b></p>
              <p><span>严重等级:</span><b>{{ eventDetail.severityText }}</b></p>
              <p><span>发生时间:</span><b>{{ eventDetail.time }}</b></p>
              <p><span>当前状态:</span><b>{{ eventDetail.statusText }}</b></p>
            </div>
            <div class="manual36-info-card">
              <h4>当前指标</h4>
              <p><span>触发值:</span><b>{{ eventDetail.triggerValue }}</b></p>
              <p><span>阈值:</span><b>{{ eventDetail.thresholdValue }}</b></p>
              <p><span>体温:</span><b>{{ metricValue(eventDetail.currentTemperature, '℃') }}</b></p>
              <p><span>心率:</span><b>{{ metricValue(eventDetail.currentHeartRate, ' bpm') }}</b></p>
              <p><span>活动量:</span><b>{{ metricValue(eventDetail.currentActivity) }}</b></p>
            </div>
          </div>
          <div class="manual36-analysis">
            <h4>处理记录</h4>
            <p v-for="record in eventProcessRows" :key="`${record.stage}-${record.time}`">
              <b></b>
              <span>{{ record.stage }}：{{ record.note }}（{{ record.operator || '系统' }} {{ formatTime(record.time) }}）</span>
            </p>
          </div>
          <div class="manual36-footer">
            <button class="manual36-btn manual36-btn--primary" @click="$emit('close')">关闭</button>
          </div>
        </template>
      </section>
    </div>
</template>

<script setup>
defineProps({
  eventDetail: { type: Object, required: true },
  loading: { type: Boolean, default: false },
  eventProcessRows: { type: Array, default: () => [] },
  formatAnimalId: { type: Function, required: true },
  metricValue: { type: Function, required: true },
  formatTime: { type: Function, required: true }
})

defineEmits(['close'])
</script>
