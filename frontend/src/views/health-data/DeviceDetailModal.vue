<template>
  <div class="manual32-overlay" @click.self="$emit('close')">
    <section class="manual32-modal manual32-modal--detail">
      <header class="manual32-modal-header">
        <h3>{{ detail.deviceId ? `${detail.deviceId} 详情` : '设备详情' }}</h3>
        <button type="button" @click="$emit('close')">×</button>
      </header>
      <div class="manual32-modal-body">
        <div v-if="loading" class="manual-empty">加载设备详情中...</div>
        <template v-else>
          <div class="manual32-detail-grid">
            <section class="manual32-info-card">
              <h4>设备信息</h4>
              <dl>
                <div><dt>设备ID:</dt><dd>{{ detail.deviceId }}</dd></div>
                <div><dt>牲畜编号:</dt><dd>{{ detail.animalId }}</dd></div>
                <div><dt>设备型号:</dt><dd>{{ cleanText(detail.deviceModel, 'SmartCollar V2') }}</dd></div>
                <div><dt>固件版本:</dt><dd>{{ cleanText(detail.firmwareVersion, 'v2.1.3') }}</dd></div>
                <div><dt>安装日期:</dt><dd>{{ installDateText }}</dd></div>
              </dl>
            </section>
            <section class="manual32-info-card">
              <h4>健康参数</h4>
              <dl>
                <div><dt>体温:</dt><dd class="manual32-green">{{ metricValue(detail.currentTemperature, '℃') }}</dd></div>
                <div><dt>心率:</dt><dd>{{ metricValue(detail.currentHeartRate, ' bpm') }}</dd></div>
                <div><dt>活动量:</dt><dd>{{ activityLabel(detail.currentActivity) }}</dd></div>
                <div><dt>电量:</dt><dd class="manual32-green">{{ batteryPercent(detail.batteryLevel) }}%</dd></div>
                <div><dt>信号强度:</dt><dd>{{ signalText(detail.signalStrength) }}</dd></div>
              </dl>
            </section>
          </div>

          <section class="manual32-trend">
            <h4>近期健康趋势</h4>
            <table class="manual32-mini-table">
              <thead>
                <tr>
                  <th>时间</th>
                  <th>体温</th>
                  <th>心率</th>
                  <th>活动量</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="log in compactLogs" :key="`${log.time}-${log.temperature}-${log.heartRate}`">
                  <td>{{ compactTime(log.time) }}</td>
                  <td>{{ metricValue(log.temperature, '℃') }}</td>
                  <td>{{ metricValue(log.heartRate, ' bpm') }}</td>
                  <td>{{ activityLabel(log.activityLevel) }}</td>
                  <td><span class="manual32-status-chip">{{ log.status || '正常' }}</span></td>
                </tr>
                <tr v-if="!compactLogs.length">
                  <td colspan="5" class="manual-empty">暂无健康趋势</td>
                </tr>
              </tbody>
            </table>
          </section>
        </template>
      </div>
      <footer class="manual32-modal-footer">
        <button class="manual32-btn" @click="$emit('close')">关闭</button>
        <button class="manual32-btn manual32-btn--success" :disabled="!detail.deviceId" @click="$emit('config')">配置</button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { cleanText, metricValue } from '@/utils/formatters'
import { activityLabel, batteryPercent, compactTime, signalText } from '@/composables/useHealthDataPage'

defineProps({
  detail: {
    type: Object,
    default: () => ({})
  },
  loading: {
    type: Boolean,
    default: false
  },
  compactLogs: {
    type: Array,
    default: () => []
  },
  installDateText: {
    type: String,
    default: '-'
  }
})

defineEmits(['close', 'config'])
</script>

<style src="./health-data-modal.css"></style>
