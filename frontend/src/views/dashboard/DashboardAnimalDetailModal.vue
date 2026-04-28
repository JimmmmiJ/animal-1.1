<template>
<div class="manual36-mask" @click.self="$emit('close')">
      <section class="manual36-modal manual36-modal--detail">
        <button class="manual36-close" @click="$emit('close')">×</button>
        <h3>{{ formatAnimalId(animalDetail.animalId) }} 健康详情</h3>
        <div v-if="animalDetailLoading" class="manual36-empty">加载详情中...</div>
        <template v-else>
          <div class="manual36-detail-grid">
            <div class="manual36-info-card">
              <h4>基本信息</h4>
              <p><span>年龄:</span><b>{{ metricValue(animalDetail.age, '岁') }}</b></p>
              <p><span>品种:</span><b>{{ animalDetail.breed || '-' }}</b></p>
              <p><span>体重:</span><b>{{ metricValue(animalDetail.weight, 'kg') }}</b></p>
              <p><span>入栏日期:</span><b>{{ animalDetail.entryDate || '-' }}</b></p>
              <p><span>所属分组:</span><b>{{ dashboardGroup }}</b></p>
            </div>
            <div class="manual36-info-card">
              <h4>实时健康指标</h4>
              <p><span>健康评分:</span><b :class="scoreClass(animalDetail.healthScore)">{{ animalDetail.healthScore }}</b></p>
              <p><span>体温:</span><b>{{ metricValue(animalDetail.currentTemperature, '℃') }}</b></p>
              <p><span>心率:</span><b>{{ metricValue(animalDetail.currentHeartRate, ' bpm') }}</b></p>
              <p><span>活动量:</span><b>{{ activityText(animalDetail.currentActivity) }}</b></p>
              <p><span>反刍时长:</span><b>{{ ruminationDetailText }}</b></p>
            </div>
            <div class="manual36-info-card">
              <h4>风险评估</h4>
              <p><span>总体风险:</span><b>{{ riskLevelText(animalDetail.riskLevel) }}</b></p>
              <p><span>疾病风险:</span><b>{{ riskLevelText(animalDetail.riskLevel) }}</b></p>
              <p><span>繁殖状态:</span><b>{{ estrusText(animalDetail.estrusStatus) }}</b></p>
              <p><span>营养状况:</span><b>正常</b></p>
              <p><span>应激水平:</span><b>中</b></p>
            </div>
          </div>
          <div class="manual36-insight-grid">
            <div class="manual36-soft-card">
              <h4>健康趋势</h4>
              <ChartBox :option="animalHealthTrendOption" height="190px" />
            </div>
            <div class="manual36-soft-card">
              <h4>行为模式</h4>
              <ChartBox :option="animalBehaviorOption" height="190px" />
            </div>
          </div>
          <div class="manual36-footer">
            <button class="manual36-btn" @click="$emit('close')">关闭</button>
            <button class="manual36-btn manual36-btn--green" @click="$emit('intervention')">健康干预</button>
          </div>
        </template>
      </section>
    </div>
</template>

<script setup>
import ChartBox from '@/components/common/ChartBox.vue'

defineProps({
  animalDetail: { type: Object, required: true },
  loading: { type: Boolean, default: false },
  dashboardGroup: { type: String, default: '' },
  ruminationDetailText: { type: String, default: '' },
  animalHealthTrendOption: { type: Object, required: true },
  animalBehaviorOption: { type: Object, required: true },
  formatAnimalId: { type: Function, required: true },
  metricValue: { type: Function, required: true },
  scoreClass: { type: Function, required: true },
  activityText: { type: Function, required: true },
  riskLevelText: { type: Function, required: true },
  estrusText: { type: Function, required: true }
})

defineEmits(['close', 'intervention'])
</script>
