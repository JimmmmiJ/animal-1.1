<template>
<div class="manual37-overlay">
      <section class="manual37-modal manual37-modal--rule">
        <button class="manual37-close" @click="$emit('close')">×</button>
        <h3>创建告警规则</h3>
        <div class="manual37-form-grid">
          <label class="manual37-field">
            <span>规则名称</span>
            <input v-model="ruleForm.name" />
          </label>
          <label class="manual37-field">
            <span>触发条件</span>
            <select v-model="ruleForm.condition">
              <option value="体温异常">体温异常</option>
              <option value="心率异常">心率异常</option>
              <option value="活动异常">活动异常</option>
              <option value="设备故障">设备故障</option>
            </select>
          </label>
          <label class="manual37-field">
            <span>严重等级</span>
            <select v-model="ruleForm.severity">
              <option value="high">高</option>
              <option value="medium">中</option>
              <option value="low">低</option>
            </select>
          </label>
          <label class="manual37-field">
            <span>推送渠道</span>
            <select v-model="ruleForm.pushChannel">
              <option value="wechat">微信推送</option>
              <option value="sms">短信推送</option>
              <option value="wechat,sms">微信+短信</option>
            </select>
          </label>
          <label class="manual37-field">
            <span>阈值设置</span>
            <input v-model="ruleForm.minValue" placeholder="最小值" />
          </label>
          <label class="manual37-field manual37-field--inline">
            <span>&nbsp;</span>
            <input v-model="ruleForm.maxValue" placeholder="最大值" />
          </label>
          <label class="manual37-field">
            <span>生效时间</span>
            <input v-model="ruleForm.activeStart" type="time" />
          </label>
          <label class="manual37-field manual37-field--inline">
            <span>&nbsp;</span>
            <input v-model="ruleForm.activeEnd" type="time" />
          </label>
          <label class="manual37-field manual37-field--full">
            <span>备注说明</span>
            <textarea v-model="ruleForm.note"></textarea>
          </label>
        </div>
        <div class="manual37-footer">
          <button class="manual37-btn" @click="$emit('close')">取消</button>
          <button class="manual37-btn manual37-btn--primary" :disabled="savingAction === 'rule'" @click="$emit('save')">
            {{ savingAction === 'rule' ? '创建中...' : '创建规则' }}
          </button>
        </div>
      </section>
    </div>
</template>

<script setup>
defineProps({
  ruleForm: { type: Object, required: true },
  savingAction: { type: String, default: '' }
})

defineEmits(['close', 'save'])
</script>
