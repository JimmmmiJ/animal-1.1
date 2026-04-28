<template>
  <div class="manual-page health32-page">
    <PageHeader title="牲畜健康数据实时采集管理">
      <template #actions>
        <button class="manual32-top-btn manual32-top-btn--primary" @click="openGlobalConfig">⚙ 设备配置</button>
        <button class="manual32-top-btn" @click="openSyncModal">🔄 数据同步</button>
        <button class="manual32-top-btn" @click="openHealthReport">📊 健康报告</button>
      </template>
    </PageHeader>

    <div class="manual32-card-grid">
      <section class="manual32-stat-card">
        <div class="manual32-stat-head">
          <span>在线设备</span>
          <em class="manual32-tag manual32-tag--green">活跃</em>
        </div>
        <strong>{{ summary.onlineDevices }}</strong>
        <div class="manual32-stat-row">
          <span>总设备数</span>
          <b>{{ summary.baseline.totalDevices }}</b>
        </div>
        <div class="manual32-progress">
          <i :style="{ width: `${deviceRate(summary.onlineDevices)}%` }" />
        </div>
        <div class="manual32-stat-row">
          <span>离线设备</span>
          <b>{{ summary.offlineDevices }}</b>
        </div>
      </section>

      <section class="manual32-stat-card">
        <div class="manual32-stat-head">
          <span>数据更新频率</span>
          <em class="manual32-tag manual32-tag--blue">实时</em>
        </div>
        <strong>{{ uploadFrequencyText }}</strong>
        <div class="manual32-stat-row">
          <span>上次更新</span>
          <b>{{ lastUpdateShort }}</b>
        </div>
        <div class="manual32-progress manual32-progress--blue">
          <i :style="{ width: `${completionRate}%` }" />
        </div>
        <div class="manual32-stat-row">
          <span>今日采集</span>
          <b>{{ todayCollectText }}</b>
        </div>
      </section>

      <section class="manual32-stat-card">
        <div class="manual32-stat-head">
          <span>异常设备</span>
          <em class="manual32-tag manual32-tag--yellow">待处理</em>
        </div>
        <strong>{{ abnormalDeviceCount }}</strong>
        <div class="manual32-stat-row">
          <span>预警数量</span>
          <b>{{ warningCount }}</b>
        </div>
        <div class="manual32-progress manual32-progress--yellow">
          <i :style="{ width: `${deviceRate(abnormalDeviceCount)}%` }" />
        </div>
        <div class="manual32-stat-row">
          <span>故障设备</span>
          <b>{{ summary.faultDevices }}</b>
        </div>
      </section>

      <section class="manual32-stat-card">
        <div class="manual32-stat-head">
          <span>健康评分</span>
          <em class="manual32-tag manual32-tag--purple">良好</em>
        </div>
        <strong>{{ scoreText }}</strong>
        <div class="manual32-stat-row">
          <span>平均体温</span>
          <b>{{ avgTemperatureText }}</b>
        </div>
        <div class="manual32-progress manual32-progress--purple">
          <i :style="{ width: `${healthScoreRate}%` }" />
        </div>
        <div class="manual32-stat-row">
          <span>活跃度</span>
          <b>{{ activityPercentText }}</b>
        </div>
      </section>
    </div>

    <DeviceTable
      :devices="devices"
      @filter="openFilterModal"
      @export="exportDevicesCsv"
      @detail="openDeviceDetail"
      @config="openDeviceConfig"
      @replace="openReplaceDevice"
    />

    <GlobalDeviceConfigModal
      v-if="showGlobalConfigModal"
      :config="globalConfig"
      :frequency-options="frequencyOptions"
      :saving="savingAction === 'global-config'"
      @close="closeGlobalConfig"
      @save="saveGlobalConfig"
    />

    <DeviceDetailModal
      v-if="showDeviceDetailModal"
      :detail="deviceDetail"
      :loading="deviceDetailLoading"
      :compact-logs="compactLogs"
      :install-date-text="installDateText"
      @close="closeDeviceDetail"
      @config="openDeviceConfigFromDetail"
    />

    <DeviceConfigModal
      v-if="showDeviceConfigModal"
      :config="deviceConfig"
      :saving="savingAction === 'device-config'"
      @close="closeDeviceConfig"
      @save="saveDeviceConfig"
    />

    <div v-if="showSyncStatusModal" class="manual32-overlay" @click.self="showSyncStatusModal = false">
      <section class="manual32-modal manual32-modal--small">
        <header class="manual32-modal-header">
          <h3>数据同步</h3>
          <button type="button" @click="showSyncStatusModal = false">×</button>
        </header>
        <div class="manual32-modal-body">
          <p class="manual32-message">最近同步时间：{{ syncState.lastSyncTime }}</p>
        </div>
        <footer class="manual32-modal-footer">
          <button class="manual32-btn" @click="showSyncStatusModal = false">关闭</button>
          <button class="manual32-btn manual32-btn--primary" :disabled="syncState.syncing" @click="runSync">
            {{ syncState.syncing ? '同步中...' : '立即同步' }}
          </button>
        </footer>
      </section>
    </div>

    <div v-if="showHealthReportModal" class="manual32-overlay" @click.self="showHealthReportModal = false">
      <section class="manual32-modal manual32-modal--small">
        <header class="manual32-modal-header">
          <h3>健康报告</h3>
          <button type="button" @click="showHealthReportModal = false">×</button>
        </header>
        <div class="manual32-modal-body">
          <div class="manual32-report-grid">
            <div><span>健康评分</span><b>{{ scoreText }}</b></div>
            <div><span>平均体温</span><b>{{ avgTemperatureText }}</b></div>
            <div><span>在线设备</span><b>{{ summary.onlineDevices }}</b></div>
            <div><span>异常设备</span><b>{{ abnormalDeviceCount }}</b></div>
          </div>
        </div>
        <footer class="manual32-modal-footer">
          <button class="manual32-btn manual32-btn--primary" @click="showHealthReportModal = false">确定</button>
        </footer>
      </section>
    </div>

    <div v-if="showFilterModal" class="manual32-overlay" @click.self="closeFilterModal">
      <section class="manual32-modal manual32-modal--small">
        <header class="manual32-modal-header">
          <h3>筛选条件</h3>
          <button type="button" @click="closeFilterModal">×</button>
        </header>
        <div class="manual32-modal-body">
          <label class="manual32-field manual32-field--full">
            <span>设备状态</span>
            <select v-model="filterDraft">
              <option value="">全部状态</option>
              <option value="online">在线</option>
              <option value="offline">离线</option>
              <option value="fault">故障</option>
            </select>
          </label>
        </div>
        <footer class="manual32-modal-footer">
          <button class="manual32-btn" @click="closeFilterModal">取消</button>
          <button class="manual32-btn manual32-btn--primary" @click="applyFilter">确定</button>
        </footer>
      </section>
    </div>

    <div v-if="showReplaceDeviceModal" class="manual32-overlay" @click.self="closeReplaceDevice">
      <section class="manual32-modal manual32-modal--small">
        <header class="manual32-modal-header">
          <h3>更换设备</h3>
          <button type="button" @click="closeReplaceDevice">×</button>
        </header>
        <div class="manual32-modal-body">
          <p class="manual32-message">当前设备 {{ replaceDeviceTarget.deviceId }} 已进入更换登记流程。本演示环境不直接替换真实设备数据。</p>
        </div>
        <footer class="manual32-modal-footer">
          <button class="manual32-btn manual32-btn--primary" @click="closeReplaceDevice">确定</button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import PageHeader from '@/components/common/PageHeader.vue'
import { useHealthDataPage } from '@/composables/useHealthDataPage'
import DeviceTable from '@/views/health-data/DeviceTable.vue'
import GlobalDeviceConfigModal from '@/views/health-data/GlobalDeviceConfigModal.vue'
import DeviceDetailModal from '@/views/health-data/DeviceDetailModal.vue'
import DeviceConfigModal from '@/views/health-data/DeviceConfigModal.vue'

const {
  frequencyOptions,
  summary,
  devices,
  filterDraft,
  savingAction,
  showGlobalConfigModal,
  showDeviceDetailModal,
  showDeviceConfigModal,
  showSyncStatusModal,
  showHealthReportModal,
  showFilterModal,
  showReplaceDeviceModal,
  deviceDetailLoading,
  deviceDetail,
  replaceDeviceTarget,
  globalConfig,
  deviceConfig,
  syncState,
  abnormalDeviceCount,
  warningCount,
  uploadFrequencyText,
  completionRate,
  scoreText,
  healthScoreRate,
  avgTemperatureText,
  activityPercentText,
  todayCollectText,
  lastUpdateShort,
  compactLogs,
  installDateText,
  openGlobalConfig,
  closeGlobalConfig,
  saveGlobalConfig,
  openDeviceDetail,
  closeDeviceDetail,
  openDeviceConfig,
  openDeviceConfigFromDetail,
  closeDeviceConfig,
  saveDeviceConfig,
  openSyncModal,
  runSync,
  openHealthReport,
  openFilterModal,
  closeFilterModal,
  applyFilter,
  openReplaceDevice,
  closeReplaceDevice,
  exportDevicesCsv,
  deviceRate
} = useHealthDataPage()
</script>

<style scoped>
.health32-page {
  padding-bottom: 24px;
}

:deep(.manual-page-header) {
  margin-bottom: 16px;
}

:deep(.manual-page-title) {
  font-size: 22px;
  line-height: 32px;
}

:deep(.manual-page-actions) {
  gap: 10px;
}

.manual32-top-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 108px;
  height: 36px;
  padding: 0 16px;
  border: 1px solid #d8dee8;
  border-radius: 5px;
  background: #fff;
  color: #31435f;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.manual32-top-btn--primary {
  border-color: #143b91;
  background: #173f98;
  color: #fff;
}

.manual32-card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 22px;
}

.manual32-stat-card {
  min-height: 138px;
  padding: 18px 22px 16px;
  border: 1px solid #dfe6f0;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.08);
}

.manual32-stat-head,
.manual32-stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #708097;
  font-size: 13px;
}

.manual32-stat-card strong {
  display: block;
  margin-top: 5px;
  color: #111827;
  font-size: 25px;
  line-height: 32px;
  font-weight: 700;
}

.manual32-stat-row {
  margin-top: 8px;
}

.manual32-stat-row b {
  color: #56657b;
  font-weight: 500;
}

.manual32-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  font-style: normal;
  font-size: 12px;
  font-weight: 500;
}

.manual32-tag--green {
  background: #dff8e8;
  color: #16a34a;
}

.manual32-tag--blue {
  background: #e3efff;
  color: #2563eb;
}

.manual32-tag--yellow {
  background: #fff5c7;
  color: #a16207;
}

.manual32-tag--purple {
  background: #f6e6ff;
  color: #7e22ce;
}

.manual32-progress {
  height: 5px;
  margin-top: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: #e6e9ee;
}

.manual32-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #22c55e;
}

.manual32-progress--blue i {
  background: #3b82f6;
}

.manual32-progress--yellow i {
  background: #eab308;
}

.manual32-progress--purple i {
  background: #a855f7;
}

@media (max-width: 1180px) {
  .manual32-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .manual32-card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
