<template>
<div class="manual36-mask" @click.self="$emit('close')">
      <section class="manual36-modal manual36-modal--config">
        <button class="manual36-close" @click="$emit('close')">×</button>
        <h3>看板配置</h3>
        <div class="manual36-field manual36-field--full">
          <span>显示指标</span>
          <div class="manual36-check-grid">
            <label><input v-model="dashboardConfig.showScore" type="checkbox" /> 整体健康评分</label>
            <label><input v-model="dashboardConfig.showRisk" type="checkbox" /> 风险预警</label>
            <label><input v-model="dashboardConfig.showEstrus" type="checkbox" /> 发情识别</label>
            <label><input v-model="dashboardConfig.showRumination" type="checkbox" /> 反刍效率</label>
          </div>
        </div>
        <label class="manual36-field manual36-field--full">
          <span>图表类型</span>
          <select v-model="dashboardConfig.chartType">
            <option value="line">折线图</option>
            <option value="bar">柱状图</option>
            <option value="heatmap">热力图</option>
          </select>
        </label>
        <label class="manual36-field manual36-field--full">
          <span>自动刷新间隔</span>
          <select v-model.number="dashboardConfig.refreshSeconds">
            <option :value="0">不自动刷新</option>
            <option :value="60">1分钟</option>
            <option :value="300">5分钟</option>
            <option :value="600">10分钟</option>
          </select>
        </label>
        <label class="manual36-field manual36-field--full">
          <span>默认分组</span>
          <select v-model="dashboardConfig.defaultGroup">
            <option value="全部羊群">全部羊群</option>
            <option value="重点关注">重点关注</option>
            <option value="发情母畜">发情母畜</option>
          </select>
        </label>
        <div class="manual36-footer">
          <button class="manual36-btn" @click="$emit('close')">取消</button>
          <button class="manual36-btn manual36-btn--primary" :disabled="savingAction === 'dashboard-config'" @click="$emit('save')">
            {{ savingAction === 'dashboard-config' ? '保存中...' : '保存配置' }}
          </button>
        </div>
      </section>
    </div>
</template>

<script setup>
defineProps({
  dashboardConfig: { type: Object, required: true },
  savingAction: { type: String, default: '' }
})

defineEmits(['close', 'save'])
</script>
