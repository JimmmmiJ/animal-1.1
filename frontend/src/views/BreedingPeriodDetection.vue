<template>
  <div class="manual-page manual33-page">
    <PageHeader title="发情期智能识别预警系统">
      <template #actions>
        <button class="manual33-top-btn manual33-top-btn--primary" @click="openBreedingConfig">⚙ 配种配置</button>
        <button class="manual33-top-btn" @click="openAnalysisReport">📊 分析报告</button>
        <button class="manual33-top-btn" @click="openNoticeSettings">🔔 通知设置</button>
      </template>
    </PageHeader>

    <div class="manual33-card-grid">
      <section class="manual33-stat-card">
        <div class="manual33-stat-head">
          <span>处于发情期</span>
          <b class="manual33-tag manual33-tag--pink">活跃</b>
        </div>
        <strong>{{ estrusCount }}</strong>
        <div class="manual33-stat-row"><span>总数</span><span>{{ femaleTotal }}</span></div>
        <div class="manual33-progress"><i class="manual33-progress--pink" :style="{ width: rateWidth(estrusCount, femaleTotal) }"></i></div>
        <div class="manual33-stat-row"><span>占比</span><span>{{ percentText(estrusCount, femaleTotal) }}</span></div>
      </section>

      <section class="manual33-stat-card">
        <div class="manual33-stat-head">
          <span>即将发情</span>
          <b class="manual33-tag manual33-tag--yellow">预警</b>
        </div>
        <strong>{{ approachingCount }}</strong>
        <div class="manual33-stat-row"><span>预计周期</span><span>2-3天</span></div>
        <div class="manual33-progress"><i class="manual33-progress--yellow" :style="{ width: rateWidth(approachingCount, femaleTotal) }"></i></div>
        <div class="manual33-stat-row"><span>趋势</span><span>↑ {{ trendPercent }}%</span></div>
      </section>

      <section class="manual33-stat-card">
        <div class="manual33-stat-head">
          <span>妊娠期</span>
          <b class="manual33-tag manual33-tag--green">安全</b>
        </div>
        <strong>{{ pregnantCount }}</strong>
        <div class="manual33-stat-row"><span>平均孕龄</span><span>16周</span></div>
        <div class="manual33-progress"><i class="manual33-progress--green" :style="{ width: rateWidth(pregnantCount, femaleTotal) }"></i></div>
        <div class="manual33-stat-row"><span>分娩预警</span><span>{{ deliveryWarningCount }}</span></div>
      </section>

      <section class="manual33-stat-card">
        <div class="manual33-stat-head">
          <span>健康评分</span>
          <b class="manual33-tag manual33-tag--purple">{{ healthGrade }}</b>
        </div>
        <strong>{{ healthScore }}</strong>
        <div class="manual33-stat-row"><span>准确率</span><span>{{ accuracyText }}</span></div>
        <div class="manual33-progress"><i class="manual33-progress--purple" :style="{ width: accuracyText }"></i></div>
        <div class="manual33-stat-row"><span>召回率</span><span>{{ recallText }}</span></div>
      </section>
    </div>

    <EstrusResultTable
      :animals="animals"
      :display-animal-code="displayAnimalCode"
      :status-tone="statusTone"
      :status-text="statusText"
      :probability="probability"
      :stage-text="stageText"
      :start-time-text="startTimeText"
      :end-time-text="endTimeText"
      @filter="openFilterModal"
      @export="exportEstrusCsv"
      @detail="openAnimalDetail"
      @plan="openBreedingPlan"
      @history="openHistory"
    />

    <div v-if="activeModal" class="manual33-overlay" @click.self="closeModal">
      <BreedingConfigModal
        v-if="activeModal === 'config'"
        :config="breedingConfig"
        :saving="savingAction === 'config'"
        @close="closeModal"
        @save="saveBreedingConfig"
      />

      <EstrusDetailModal
        v-else-if="activeModal === 'detail'"
        :detail="detail"
        :loading="detailLoading"
        :selected-animal-code="selectedAnimalCode"
        :activity-rows="activityRows"
        :status-text="statusText"
        :probability="probability"
        :stage-text="stageText"
        :end-time-text="endTimeText"
        @close="closeModal"
        @plan="openBreedingPlan"
      />

      <BreedingPlanModal
        v-else-if="activeModal === 'plan'"
        :form="planForm"
        :selected-animal-code="selectedAnimalCode"
        :saving="savingAction === 'plan'"
        @close="closeModal"
        @save="saveBreedingPlan"
      />

      <section v-else-if="activeModal === 'filter'" class="manual33-modal manual33-modal--small">
        <header class="manual33-modal-header">
          <h3>筛选条件</h3>
          <button type="button" @click="closeModal">×</button>
        </header>
        <div class="manual33-modal-body">
          <label class="manual33-field manual33-field--full">
            <span>发情状态</span>
            <select v-model="statusFilterDraft">
              <option value="">全部状态</option>
              <option value="estrus">发情期</option>
              <option value="approaching">即将发情</option>
              <option value="pregnant">妊娠期</option>
              <option value="normal">非发情期</option>
            </select>
          </label>
        </div>
        <footer class="manual33-modal-footer">
          <button class="manual33-btn" @click="resetFilter">重置</button>
          <button class="manual33-btn manual33-btn--primary" @click="applyFilter">确定</button>
        </footer>
      </section>

      <section v-else-if="activeModal === 'history'" class="manual33-modal manual33-modal--small">
        <header class="manual33-modal-header">
          <h3>{{ selectedAnimalCode }} 历史</h3>
          <button type="button" @click="closeModal">×</button>
        </header>
        <div class="manual33-modal-body">
          <table class="manual33-inner-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>项目</th>
                <th>结果</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in historyRows" :key="`${row.time}-${row.item}`">
                <td>{{ row.time }}</td>
                <td>{{ row.item }}</td>
                <td>{{ row.value }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <footer class="manual33-modal-footer">
          <button class="manual33-btn manual33-btn--primary" @click="closeModal">关闭</button>
        </footer>
      </section>

      <section v-else-if="activeModal === 'analysis'" class="manual33-modal manual33-modal--small">
        <header class="manual33-modal-header">
          <h3>分析报告</h3>
          <button type="button" @click="closeModal">×</button>
        </header>
        <div class="manual33-modal-body">
          <div class="manual33-report-grid">
            <div><span>处于发情期</span><b>{{ estrusCount }}</b></div>
            <div><span>即将发情</span><b>{{ approachingCount }}</b></div>
            <div><span>妊娠期</span><b>{{ pregnantCount }}</b></div>
            <div><span>健康评分</span><b>{{ healthScore }}</b></div>
          </div>
          <p class="manual33-message">当前发情识别阈值为 {{ breedingConfig.probabilityThreshold }}%，系统按实时活动量与发情概率生成配种建议。</p>
        </div>
        <footer class="manual33-modal-footer">
          <button class="manual33-btn manual33-btn--primary" @click="closeModal">确定</button>
        </footer>
      </section>

      <section v-else-if="activeModal === 'notice'" class="manual33-modal manual33-modal--small">
        <header class="manual33-modal-header">
          <h3>通知设置</h3>
          <button type="button" @click="closeModal">×</button>
        </header>
        <div class="manual33-modal-body">
          <div class="manual33-form-grid">
            <label class="manual33-field">
              <span>通知方式</span>
              <select v-model="noticeConfig.channel">
                <option value="system">站内通知</option>
                <option value="sms">短信通知</option>
                <option value="wechat">微信通知</option>
              </select>
            </label>
            <label class="manual33-field">
              <span>提醒阈值(%)</span>
              <input v-model.number="noticeConfig.probabilityThreshold" type="number" />
            </label>
            <label class="manual33-field">
              <span>提前提醒(小时)</span>
              <input v-model.number="noticeConfig.noticeHours" type="number" />
            </label>
            <label class="manual33-field">
              <span>通知频率</span>
              <select v-model="noticeConfig.frequency">
                <option value="once">仅提醒一次</option>
                <option value="daily">每日提醒</option>
                <option value="realtime">实时提醒</option>
              </select>
            </label>
          </div>
        </div>
        <footer class="manual33-modal-footer">
          <button class="manual33-btn" @click="closeModal">取消</button>
          <button class="manual33-btn manual33-btn--primary" :disabled="savingAction === 'notice'" @click="saveNoticeSettings">
            {{ savingAction === 'notice' ? '保存中...' : '保存配置' }}
          </button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import PageHeader from '@/components/common/PageHeader.vue'
import { useBreedingPage } from '@/composables/useBreedingPage'
import EstrusResultTable from '@/views/breeding/EstrusResultTable.vue'
import BreedingConfigModal from '@/views/breeding/BreedingConfigModal.vue'
import EstrusDetailModal from '@/views/breeding/EstrusDetailModal.vue'
import BreedingPlanModal from '@/views/breeding/BreedingPlanModal.vue'

const {
  animals,
  activeModal,
  savingAction,
  statusFilterDraft,
  detail,
  detailLoading,
  activityRows,
  breedingConfig,
  noticeConfig,
  planForm,
  femaleTotal,
  estrusCount,
  approachingCount,
  pregnantCount,
  deliveryWarningCount,
  healthScore,
  healthGrade,
  accuracyText,
  recallText,
  trendPercent,
  selectedAnimalCode,
  historyRows,
  openBreedingConfig,
  saveBreedingConfig,
  openAnalysisReport,
  openNoticeSettings,
  saveNoticeSettings,
  openFilterModal,
  applyFilter,
  resetFilter,
  openAnimalDetail,
  openBreedingPlan,
  saveBreedingPlan,
  openHistory,
  closeModal,
  probability,
  statusText,
  stageText,
  statusTone,
  displayAnimalCode,
  startTimeText,
  endTimeText,
  rateWidth,
  percentText,
  exportEstrusCsv
} = useBreedingPage()
</script>

<style src="./breeding/breeding-page.css"></style>
