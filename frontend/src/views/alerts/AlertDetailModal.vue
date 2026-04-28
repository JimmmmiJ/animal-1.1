<template>
<div class="manual37-overlay">
      <section class="manual37-modal manual37-modal--detail">
        <button class="manual37-close" @click="$emit('close')">×</button>
        <h3>{{ detail.alertId ? `${detail.alertId} - 告警详情` : '告警详情' }}</h3>
        <div v-if="loading" class="manual37-empty manual37-empty--block">加载详情中...</div>
        <template v-else>
          <div class="manual37-info-grid">
            <div class="manual37-soft-card">
              <h4>告警信息</h4>
              <p><span>告警ID:</span><b>{{ detail.alertId || '-' }}</b></p>
              <p><span>牲畜编号:</span><b>{{ formatAnimalId(detail.animalId) }}</b></p>
              <p><span>告警类型:</span><b>{{ typeText(detail.type) }}</b></p>
              <p><span>严重等级:</span><b class="manual37-red-text">{{ severityText(detail.severity) }}</b></p>
              <p><span>触发时间:</span><b>{{ detail.time || '-' }}</b></p>
            </div>
            <div class="manual37-soft-card">
              <h4>设备信息</h4>
              <p><span>设备ID:</span><b>{{ detail.deviceId || '-' }}</b></p>
              <p><span>设备状态:</span><b>{{ deviceStatusText(detail.deviceStatus) }}</b></p>
              <p><span>信号强度:</span><b>{{ detailSignalText }}</b></p>
              <p><span>电量:</span><b>{{ detailBatteryText }}</b></p>
              <p><span>推送渠道:</span><b>{{ detailPushChannelText }}</b></p>
            </div>
          </div>

          <h4 class="manual37-section-title">告警数据</h4>
          <div class="manual37-metric-grid">
            <div v-for="card in alertMetricCards" :key="card.label" class="manual37-metric-card" :class="card.tone">
              <span>{{ card.label }}</span>
              <strong>{{ card.value }}</strong>
              <em>{{ card.sub }}</em>
            </div>
          </div>

          <h4 class="manual37-section-title">处理记录</h4>
          <div class="manual37-record">
            <div v-for="record in detailProcessRecords" :key="`${record.stage}-${record.time}`" class="manual37-record-row">
              <div>
                <strong>{{ record.stage }}</strong>
                <p>{{ record.note }}</p>
                <small>{{ record.operator }}</small>
              </div>
              <span>{{ formatTime(record.time) }}</span>
            </div>
          </div>

          <div class="manual37-footer">
            <button class="manual37-btn" @click="$emit('close')">关闭</button>
            <button
              class="manual37-btn manual37-btn--orange"
              :disabled="detail.status !== 'pending' || savingAction === `ack-${detail.alertId}`"
              @click="$emit('acknowledge')"
            >
              确认
            </button>
            <button
              class="manual37-btn manual37-btn--green"
              :disabled="detail.status === 'resolved' || savingAction === `resolve-${detail.alertId}`"
              @click="$emit('resolve')"
            >
              解决
            </button>
          </div>
        </template>
      </section>
    </div>
</template>

<script setup>
defineProps({
  detail: { type: Object, required: true },
  loading: { type: Boolean, default: false },
  savingAction: { type: String, default: '' },
  alertMetricCards: { type: Array, default: () => [] },
  detailProcessRecords: { type: Array, default: () => [] },
  detailSignalText: { type: String, default: '' },
  detailBatteryText: { type: String, default: '' },
  detailPushChannelText: { type: String, default: '' },
  formatAnimalId: { type: Function, required: true },
  typeText: { type: Function, required: true },
  severityText: { type: Function, required: true },
  deviceStatusText: { type: Function, required: true },
  formatTime: { type: Function, required: true }
})

defineEmits(['close', 'acknowledge', 'resolve'])
</script>
