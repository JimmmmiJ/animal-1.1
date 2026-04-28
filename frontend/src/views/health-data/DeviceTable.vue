<template>
  <PanelCard title="智能项圈设备状态">
    <template #actions>
      <button class="manual32-panel-btn" @click="$emit('filter')">筛选</button>
      <button class="manual32-panel-btn" @click="$emit('export')">导出</button>
    </template>

    <div class="manual-table-wrap manual-table-scroll manual32-table-wrap">
      <table class="manual-table manual32-table">
        <thead>
          <tr>
            <th>设备ID</th>
            <th>牲畜编号</th>
            <th>状态</th>
            <th>最后更新</th>
            <th>体温(℃)</th>
            <th>心率(BPM)</th>
            <th>活动量</th>
            <th>电量</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="device in devices" :key="device.deviceId">
            <td class="manual-cell-strong">{{ device.deviceId }}</td>
            <td>{{ device.animalId }}</td>
            <td>
              <span class="manual32-dot" :class="`manual32-dot--${device.status}`" />
              {{ device.statusText }}
            </td>
            <td>{{ compactDateTime(device.lastDataUpdateText) }}</td>
            <td>{{ metricValue(device.currentTemperature) }}</td>
            <td>{{ metricValue(device.currentHeartRate) }}</td>
            <td>{{ activityLabel(device.currentActivity) }}</td>
            <td>
              <div class="manual32-battery">
                <span><i :class="batteryTone(device.batteryLevel)" :style="{ width: `${batteryPercent(device.batteryLevel)}%` }" /></span>
                <b>{{ batteryPercent(device.batteryLevel) }}%</b>
              </div>
            </td>
            <td>
              <div class="manual32-row-actions">
                <button class="manual-link manual-link--primary" @click="$emit('detail', device)">详情</button>
                <button class="manual-link manual-link--success" @click="$emit('config', device)">配置</button>
                <button class="manual-link manual-link--warning" @click="$emit('replace', device)">更换</button>
              </div>
            </td>
          </tr>
          <tr v-if="!devices.length">
            <td colspan="9" class="manual-empty">暂无设备数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  </PanelCard>
</template>

<script setup>
import PanelCard from '@/components/common/PanelCard.vue'
import { compactDateTime, metricValue } from '@/utils/formatters'
import { activityLabel, batteryPercent, batteryTone } from '@/composables/useHealthDataPage'

defineProps({
  devices: {
    type: Array,
    default: () => []
  }
})

defineEmits(['filter', 'export', 'detail', 'config', 'replace'])
</script>

<style scoped>
:deep(.manual-panel-header) {
  min-height: 52px;
  padding: 0 18px;
}

:deep(.manual-panel-title) {
  font-size: 17px;
}

.manual32-panel-btn {
  min-width: 48px;
  height: 30px;
  padding: 0 12px;
  border: 1px solid #d7dde8;
  border-radius: 4px;
  background: #fff;
  color: #31435f;
  font-size: 13px;
}

.manual32-table-wrap {
  max-height: 500px;
}

.manual32-table th {
  height: 38px;
  white-space: nowrap;
}

.manual32-table td {
  height: 44px;
  white-space: nowrap;
}

.manual32-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 7px;
  border-radius: 50%;
  vertical-align: 1px;
  background: #94a3b8;
}

.manual32-dot--online {
  background: #059669;
}

.manual32-dot--offline {
  background: #b91c1c;
}

.manual32-dot--fault {
  background: #d97706;
}

.manual32-battery {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.manual32-battery span {
  width: 54px;
  height: 7px;
  overflow: hidden;
  border-radius: 99px;
  background: #d9dde4;
}

.manual32-battery i {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.manual32-battery b {
  min-width: 34px;
  color: #5b6678;
  font-weight: 500;
}

.manual32-battery-high {
  background: #149447;
}

.manual32-battery-mid {
  background: #b89600;
}

.manual32-battery-low {
  background: #b91c1c;
}

.manual32-row-actions {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}

.manual32-row-actions .manual-link {
  font-size: 13px;
}

.manual-link--warning {
  color: #c2410c;
}
</style>
