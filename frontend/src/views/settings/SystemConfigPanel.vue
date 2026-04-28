<template>
<section class="manual38-config-grid">
      <div class="manual38-config-column">
        <div class="manual38-config-group">
          <h2>数据采集配置</h2>
          <label class="manual38-field">
            <span>数据上传频率</span>
            <select v-model="configDraft.data_upload_interval">
              <option value="60">1分钟</option>
              <option value="300">5分钟</option>
              <option value="600">10分钟</option>
              <option value="1800">30分钟</option>
            </select>
          </label>
          <label class="manual38-field">
            <span>数据保留天数</span>
            <input v-model="configDraft.data_retention_days" type="number" />
          </label>
          <label class="manual38-field">
            <span>设备连接超时(秒)</span>
            <input v-model="configDraft.device_signal_min" type="number" />
          </label>
        </div>

        <div class="manual38-config-group">
          <h2>健康监测配置</h2>
          <label class="manual38-field">
            <span>体温报警阈值(℃)</span>
            <input v-model="configDraft.temperature_max" type="number" step="0.1" />
          </label>
          <label class="manual38-field">
            <span>心率报警阈值(bpm)</span>
            <input v-model="configDraft.heart_rate_max" type="number" />
          </label>
          <label class="manual38-field">
            <span>异常检测灵敏度</span>
            <input v-model="configDraft.behavior_sensitivity" type="range" min="1" max="3" />
            <div class="manual38-range-labels"><span>低</span><span>中</span><span>高</span></div>
          </label>
        </div>
      </div>

      <div class="manual38-config-column">
        <div class="manual38-config-group">
          <h2>告警通知配置</h2>
          <label class="manual38-field">
            <span>告警级别</span>
            <select v-model="configDraft.system_alert_level">
              <option value="高级">高级</option>
              <option value="中级">中级</option>
              <option value="低级">低级</option>
            </select>
          </label>
          <label class="manual38-field">
            <span>告警接收人</span>
            <select v-model="configDraft.system_alert_receivers" multiple>
              <option v-for="user in userList" :key="user.id" :value="user.displayName">{{ user.displayName }}</option>
            </select>
          </label>
          <div class="manual38-field">
            <span>通知方式</span>
            <label class="manual38-checkline">
              <input v-model="configDraft.email_enabled" type="checkbox" />
              邮件通知
            </label>
            <label class="manual38-checkline">
              <input v-model="configDraft.sms_enabled" type="checkbox" />
              短信通知
            </label>
            <label class="manual38-checkline">
              <input v-model="configDraft.wechat_enabled" type="checkbox" />
              微信推送
            </label>
          </div>
        </div>

        <div class="manual38-config-group">
          <h2>系统安全配置</h2>
          <label class="manual38-field">
            <span>登录失败锁定次数</span>
            <input v-model="configDraft.login_lock_attempts" type="number" />
          </label>
          <label class="manual38-field">
            <span>密码有效期(天)</span>
            <input v-model="configDraft.password_expire_days" type="number" />
          </label>
          <label class="manual38-field">
            <span>会话超时(分钟)</span>
            <input v-model="configDraft.session_timeout_minutes" type="number" />
          </label>
        </div>
      </div>

      <div class="manual38-config-footer">
        <button class="manual38-btn" @click="$emit('reset')">取消</button>
        <button class="manual38-btn manual38-btn--primary" :disabled="savingAction === 'config'" @click="$emit('save')">
          {{ savingAction === 'config' ? '保存中...' : '保存配置' }}
        </button>
      </div>
    </section>
</template>

<script setup>
defineProps({
  configDraft: { type: Object, required: true },
  userList: { type: Array, default: () => [] },
  savingAction: { type: String, default: '' }
})

defineEmits(['reset', 'save'])
</script>
