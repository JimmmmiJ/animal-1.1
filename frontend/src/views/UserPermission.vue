<template>
  <div class="manual-page manual38-page">
    <PageHeader title="用户权限与系统配置管理">
      <template #actions>
        <button class="manual38-top-btn manual38-top-btn--primary" @click="openCreateUserModal">👤 新增用户</button>
        <button class="manual38-top-btn" @click="activeTab = 'roles'">🔐 角色管理</button>
        <button class="manual38-top-btn" @click="activeTab = 'config'">⚙ 系统配置</button>
      </template>
    </PageHeader>

    <div v-if="notice.message" :class="['manual38-notice', `manual38-notice--${notice.tone}`]">
      {{ notice.message }}
    </div>

    <nav class="manual38-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </nav>

    <UserManagementPanel
      v-if="activeTab === 'users'"
      :users="filteredUsers"
      :user-keyword="userKeyword"
      :mask-phone="maskPhone"
      :format-date="formatDate"
      @update:user-keyword="userKeyword = $event"
      @batch-import="batchImportUsers"
      @edit="editUser"
      @reset-password="promptResetPassword"
      @delete="promptDeleteUser"
    />


    <RolePermissionPanel
      v-else-if="activeTab === 'roles'"
      :role-list="roleList"
      :selected-role-code="selectedRoleCode"
      :selected-role="selectedRole"
      :permission-modules="permissionModules"
      :permission-draft="permissionDraft"
      :saving-action="savingAction"
      @select="selectRole"
      @save="saveRolePermissions"
    />


    <SystemConfigPanel
      v-else-if="activeTab === 'config'"
      :config-draft="configDraft"
      :user-list="userList"
      :saving-action="savingAction"
      @reset="loadConfigs"
      @save="saveSystemConfig"
    />


    <AuditLogPanel
      v-else
      :logs="filteredAuditLogs"
      :audit-type-options="auditTypeOptions"
      :audit-type-filter="auditTypeFilter"
      :audit-date-from="auditDateFrom"
      :audit-date-to="auditDateTo"
      :format-date-time="formatDateTime"
      @update:audit-type-filter="auditTypeFilter = $event"
      @update:audit-date-from="auditDateFrom = $event"
      @update:audit-date-to="auditDateTo = $event"
      @filter="loadAuditLogs"
    />


    <UserFormModal
      v-if="showUserModal"
      :user-form="userForm"
      :editing-user="editingUser"
      :role-list="roleList"
      :saving-action="savingAction"
      @close="showUserModal = false"
      @save="saveUser"
    />


    <ConfirmActionModal
      v-if="showConfirmModal"
      :confirm-dialog="confirmDialog"
      @close="closeConfirmDialog"
      @confirm="runConfirmAction"
    />

  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { api } from '@/api'
import { cleanText, formatDate, formatDateTime } from '@/utils/formatters'
import PageHeader from '@/components/common/PageHeader.vue'
import UserManagementPanel from './settings/UserManagementPanel.vue'
import RolePermissionPanel from './settings/RolePermissionPanel.vue'
import SystemConfigPanel from './settings/SystemConfigPanel.vue'
import AuditLogPanel from './settings/AuditLogPanel.vue'
import UserFormModal from './settings/UserFormModal.vue'
import ConfirmActionModal from './settings/ConfirmActionModal.vue'

const ROLE_ID_MAP = {
  1: 'SUPER_ADMIN',
  2: 'FARM_ADMIN',
  3: 'VETERINARIAN',
  4: 'FEEDER'
}

const ROLE_META = {
  SUPER_ADMIN: { id: 1, label: '管理员', description: '拥有系统的最高权限，可以管理所有功能' },
  FARM_ADMIN: { id: 2, label: '农场管理员', description: '负责农场整体运营和数据管理' },
  VETERINARIAN: { id: 3, label: '兽医', description: '负责健康监测、疾病评估和治疗' },
  FEEDER: { id: 4, label: '饲养员', description: '负责日常饲喂管理和数据采集' },
  TECHNICIAN: { id: 5, label: '技术员', description: '负责设备维护和技术支持' }
}

const PERMISSION_META = {
  'animal:view': { module: '牲畜管理模块', name: '牲畜查看' },
  'animal:edit': { module: '牲畜管理模块', name: '牲畜编辑' },
  'animal:delete': { module: '牲畜管理模块', name: '牲畜删除' },
  'animal:treat': { module: '牲畜管理模块', name: '治疗管理' },
  'device:view': { module: '设备管理模块', name: '设备查看' },
  'device:config': { module: '设备管理模块', name: '设备配置' },
  'alert:view': { module: '告警管理模块', name: '告警查看' },
  'alert:handle': { module: '告警管理模块', name: '告警处理' },
  'user:view': { module: '用户管理模块', name: '用户管理' },
  'user:manage': { module: '用户管理模块', name: '角色管理' },
  'report:view': { module: '报表管理模块', name: '报表查看' },
  'report:export': { module: '报表管理模块', name: '报表导出' },
  'system:config': { module: '系统配置模块', name: '系统配置' },
  'disease:view': { module: '健康监测模块', name: '疾病预警' },
  'disease:manage': { module: '健康监测模块', name: '风险评估' },
  'behavior:view': { module: '行为分析模块', name: '行为查看' },
  'feeding:manage': { module: '行为分析模块', name: '饲喂调整' }
}

const DEFAULT_USERS = ['王明轩', '李晓燕', '陈志强', '赵美华', '刘德华', '孙丽娟', '周建国', '吴晓红', '马腾飞', '胡静怡']

const tabs = [
  { key: 'users', label: '用户管理' },
  { key: 'roles', label: '角色权限' },
  { key: 'config', label: '系统配置' },
  { key: 'audit', label: '审计日志' }
]

const activeTab = ref('users')
const notice = ref({ message: '', tone: 'success' })
const savingAction = ref('')

const userList = ref([])
const roleList = ref([])
const permissionCatalog = ref([])
const auditLogList = ref([])
const selectedRoleCode = ref('')

const userKeyword = ref('')
const auditTypeFilter = ref('')
const auditDateFrom = ref('')
const auditDateTo = ref('')

const showUserModal = ref(false)
const showConfirmModal = ref(false)
const editingUser = ref(null)

const permissionDraft = reactive({
  all: false,
  selected: []
})

const configDraft = reactive({
  data_upload_interval: '60',
  data_retention_days: '365',
  device_signal_min: '30',
  temperature_max: '39.5',
  heart_rate_max: '100',
  behavior_sensitivity: '2',
  system_alert_level: '高级',
  system_alert_receivers: [],
  email_enabled: true,
  sms_enabled: false,
  wechat_enabled: true,
  login_lock_attempts: '5',
  password_expire_days: '90',
  session_timeout_minutes: '30'
})

const userForm = reactive(createEmptyUserForm())
const confirmDialog = reactive({
  title: '',
  message: '',
  confirmLabel: '确定',
  tone: 'primary',
  action: null
})

const selectedRole = computed(() => roleList.value.find((role) => role.code === selectedRoleCode.value) || null)

const filteredUsers = computed(() => {
  const keyword = userKeyword.value.trim().toLowerCase()
  if (!keyword) return userList.value
  return userList.value.filter((user) => {
    return [user.displayName, user.username, user.email, user.phone].some((value) =>
      String(value || '').toLowerCase().includes(keyword)
    )
  })
})

const permissionModules = computed(() => {
  const source = permissionCatalog.value.length ? permissionCatalog.value : defaultPermissionCatalog()
  const groups = source.reduce((result, item) => {
    if (!result[item.module]) result[item.module] = []
    result[item.module].push(item)
    return result
  }, {})
  return Object.entries(groups).map(([module, items]) => ({ module, items }))
})

const auditTypeOptions = computed(() => {
  return Array.from(new Set(auditLogList.value.map((item) => item.operationType))).filter(Boolean)
})

const filteredAuditLogs = computed(() => {
  return auditLogList.value.filter((log) => {
    if (auditTypeFilter.value && log.operationType !== auditTypeFilter.value) return false
    if (auditDateFrom.value && new Date(log.createdAt) < new Date(`${auditDateFrom.value}T00:00:00`)) return false
    if (auditDateTo.value && new Date(log.createdAt) > new Date(`${auditDateTo.value}T23:59:59`)) return false
    return true
  })
})

watch(selectedRoleCode, () => {
  syncPermissionDraft(selectedRole.value)
})

function createEmptyUserForm() {
  return {
    username: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
    roleCode: 'FEEDER',
    status: 'enabled',
    remark: ''
  }
}

async function loadUsers() {
  const data = await api.system.getUsers(getFarmId())
  userList.value = Array.isArray(data) ? data.map(normalizeUser) : []
}

async function loadRoles() {
  const data = await api.system.getRoles()
  roleList.value = Array.isArray(data) ? data.map(normalizeRole) : []
  if (!selectedRoleCode.value && roleList.value.length) {
    selectedRoleCode.value = roleList.value[0].code
  }
  syncPermissionDraft(selectedRole.value)
}

async function loadPermissions() {
  const data = await api.system.getPermissions()
  permissionCatalog.value = buildPermissionCatalog(data)
}

async function loadConfigs() {
  const data = await api.system.getConfig()
  const map = Object.fromEntries((Array.isArray(data) ? data : []).map((item) => [item.key, item.value]))
  configDraft.data_upload_interval = normalizeUploadInterval(map.data_upload_interval)
  configDraft.data_retention_days = map.data_retention_days || '365'
  configDraft.device_signal_min = map.device_signal_min || '30'
  configDraft.temperature_max = map.temperature_max || '39.5'
  configDraft.heart_rate_max = map.heart_rate_max || '100'
  configDraft.behavior_sensitivity = sensitivityToSlider(map.behavior_sensitivity || '中')
  configDraft.system_alert_level = map.system_alert_level || '高级'
  configDraft.system_alert_receivers = splitList(map.system_alert_receivers || userList.value.slice(0, 4).map((user) => user.displayName).join(','))
  configDraft.email_enabled = toBoolean(map.email_enabled, true)
  configDraft.sms_enabled = toBoolean(map.sms_enabled, false)
  configDraft.wechat_enabled = toBoolean(map.wechat_enabled, true)
  configDraft.login_lock_attempts = map.login_lock_attempts || '5'
  configDraft.password_expire_days = map.password_expire_days || '90'
  configDraft.session_timeout_minutes = map.session_timeout_minutes || '30'
}

async function loadAuditLogs() {
  const data = await api.system.getAuditLogs(getFarmId(), auditTypeFilter.value, 80)
  auditLogList.value = Array.isArray(data) ? data.map(normalizeAuditLog) : []
}

function normalizeUser(raw, index = 0) {
  const id = Number(raw?.id ?? raw?.userId ?? Date.now() + index)
  const roleCode = ROLE_ID_MAP[Number(raw?.roleId)] || raw?.roleCode || 'FEEDER'
  const displayName = cleanText(raw?.realName, DEFAULT_USERS[(id - 1) % DEFAULT_USERS.length])
  const username = cleanText(raw?.username, `user${id}`)
  const email = cleanText(raw?.email, `${username}@farmtech.com`)
  const phone = cleanText(raw?.phone, `1380000${String(id).padStart(4, '0')}`)
  const status = Number(raw?.status ?? 1) === 1 ? 'enabled' : 'disabled'
  return {
    id,
    username,
    displayName,
    avatarText: displayName.slice(0, 2),
    email,
    phone,
    roleCode,
    roleId: ROLE_META[roleCode]?.id || Number(raw?.roleId) || 4,
    roleLabel: ROLE_META[roleCode]?.label || cleanText(raw?.roleName, '饲养员'),
    status,
    statusLabel: status === 'enabled' ? '激活' : '禁用',
    farmId: Number(raw?.farmId ?? getFarmId()),
    createdAt: raw?.createdAt || new Date().toISOString()
  }
}

function normalizeRole(raw) {
  const code = raw?.code || ROLE_ID_MAP[Number(raw?.id)] || 'FEEDER'
  const meta = ROLE_META[code] || ROLE_META.FEEDER
  const permissions = Array.isArray(raw?.permissions) ? [...raw.permissions] : []
  return {
    id: Number(raw?.id ?? meta.id),
    code,
    label: meta.label,
    description: meta.description,
    permissions,
    permissionCount: permissions.includes('*') ? defaultPermissionCatalog().length : permissions.length
  }
}

function buildPermissionCatalog(rawList) {
  const map = new Map()
  defaultPermissionCatalog().forEach((item) => map.set(item.code, item))
  ;(Array.isArray(rawList) ? rawList : []).forEach((item) => {
    const code = item?.code
    if (!code) return
    const meta = PERMISSION_META[code] || {}
    map.set(code, {
      code,
      module: meta.module || cleanText(item?.module, '系统配置模块'),
      name: meta.name || cleanText(item?.name, code)
    })
  })
  return Array.from(map.values())
}

function defaultPermissionCatalog() {
  return Object.entries(PERMISSION_META).map(([code, meta]) => ({ code, ...meta }))
}

function normalizeAuditLog(raw, index = 0) {
  const id = Number(raw?.id ?? Date.now() + index)
  const operationType = cleanText(raw?.operationType, ['登录操作', '数据访问', '配置变更', '用户管理'][id % 4])
  const moduleText = cleanText(raw?.operationModule, '')
  const desc = cleanText(raw?.operationDesc, auditDescription(operationType, id))
  return {
    id,
    username: auditUsername(raw?.username, id),
    operationType,
    ipAddress: raw?.ipAddress || `192.168.1.${100 + (id % 40)}`,
    operationDesc: moduleText ? `${moduleText} ${desc}` : desc,
    result: Number(raw?.responseStatus ?? 200) >= 400 ? '失败' : '成功',
    createdAt: raw?.createdAt || new Date().toISOString()
  }
}

function auditDescription(type, id) {
  if (type.includes('登录')) return id % 5 === 0 ? '尝试登录失败' : '成功登录系统'
  if (type.includes('配置')) return '修改设备配置参数'
  if (type.includes('用户')) return '创建或维护用户信息'
  return '查看健康数据报表'
}

function openCreateUserModal() {
  editingUser.value = null
  Object.assign(userForm, createEmptyUserForm())
  showUserModal.value = true
}

function editUser(user) {
  editingUser.value = user
  Object.assign(userForm, {
    username: user.username,
    email: user.email,
    phone: user.phone,
    password: '',
    confirmPassword: '',
    roleCode: user.roleCode,
    status: user.status,
    remark: ''
  })
  showUserModal.value = true
}

async function saveUser() {
  if (savingAction.value) return
  if (!userForm.username.trim()) {
    setNotice('请输入用户名。', 'danger')
    return
  }
  if (!editingUser.value && !userForm.password.trim()) {
    setNotice('新增用户需要填写密码。', 'danger')
    return
  }
  if (userForm.password || userForm.confirmPassword) {
    if (userForm.password !== userForm.confirmPassword) {
      setNotice('两次输入的密码不一致。', 'danger')
      return
    }
  }

  savingAction.value = 'user'
  const role = ROLE_META[userForm.roleCode] || ROLE_META.FEEDER
  const payload = {
    username: userForm.username.trim(),
    realName: userForm.username.trim(),
    email: userForm.email.trim(),
    phone: userForm.phone.trim(),
    password: userForm.password,
    roleId: role.id,
    roleCode: userForm.roleCode,
    farmId: Number(getFarmId()),
    status: userForm.status === 'enabled' ? 1 : 0,
    remark: userForm.remark
  }

  try {
    if (editingUser.value) {
      await api.system.updateUser(editingUser.value.id, payload)
    } else {
      await api.system.createUser(payload)
    }
    showUserModal.value = false
    await loadUsers()
    setNotice(editingUser.value ? '用户信息已保存。' : '新用户已创建。')
  } catch (error) {
    setNotice(error.message || '保存用户失败，请稍后重试。', 'danger')
  } finally {
    savingAction.value = ''
  }
}

function promptResetPassword(user) {
  Object.assign(confirmDialog, {
    title: '重置密码',
    message: `确认将 ${user.displayName} 的密码重置为默认密码吗？`,
    confirmLabel: '确认重置',
    tone: 'primary',
    action: async () => {
      await api.system.resetPassword(user.id)
      setNotice(`${user.displayName} 的密码已重置。`)
    }
  })
  showConfirmModal.value = true
}

function promptDeleteUser(user) {
  Object.assign(confirmDialog, {
    title: '删除用户',
    message: `确认删除用户 ${user.displayName} 吗？该操作会从当前演示系统中移除该账号。`,
    confirmLabel: '确认删除',
    tone: 'danger',
    action: async () => {
      await api.system.deleteUser(user.id)
      await loadUsers()
      setNotice(`${user.displayName} 已删除。`)
    }
  })
  showConfirmModal.value = true
}

async function runConfirmAction() {
  if (typeof confirmDialog.action === 'function') {
    await confirmDialog.action()
  }
  closeConfirmDialog()
}

function closeConfirmDialog() {
  showConfirmModal.value = false
  Object.assign(confirmDialog, {
    title: '',
    message: '',
    confirmLabel: '确定',
    tone: 'primary',
    action: null
  })
}

function selectRole(role) {
  selectedRoleCode.value = role.code
}

function syncPermissionDraft(role) {
  permissionDraft.all = Boolean(role?.permissions?.includes('*'))
  permissionDraft.selected = (role?.permissions || []).filter((item) => item !== '*')
}

async function saveRolePermissions() {
  if (!selectedRole.value || savingAction.value) return
  savingAction.value = 'permission'
  const permissions = permissionDraft.all ? ['*'] : Array.from(new Set(permissionDraft.selected)).sort()
  try {
    await api.system.updateConfig(`role_permissions:${selectedRole.value.code}`, JSON.stringify(permissions))
    await loadRoles()
    setNotice('权限配置已保存。')
  } catch (error) {
    setNotice(error.message || '权限保存失败，请稍后重试。', 'danger')
  } finally {
    savingAction.value = ''
  }
}

async function saveSystemConfig() {
  if (savingAction.value) return
  savingAction.value = 'config'
  const payload = {
    data_upload_interval: configDraft.data_upload_interval,
    data_retention_days: configDraft.data_retention_days,
    device_signal_min: configDraft.device_signal_min,
    temperature_max: configDraft.temperature_max,
    heart_rate_max: configDraft.heart_rate_max,
    behavior_sensitivity: sliderToSensitivity(configDraft.behavior_sensitivity),
    system_alert_level: configDraft.system_alert_level,
    system_alert_receivers: configDraft.system_alert_receivers.join(','),
    email_enabled: String(configDraft.email_enabled),
    sms_enabled: String(configDraft.sms_enabled),
    wechat_enabled: String(configDraft.wechat_enabled),
    login_lock_attempts: configDraft.login_lock_attempts,
    password_expire_days: configDraft.password_expire_days,
    session_timeout_minutes: configDraft.session_timeout_minutes
  }
  try {
    await Promise.all(Object.entries(payload).map(([key, value]) => api.system.updateConfig(key, value)))
    await loadConfigs()
    setNotice('系统配置已保存。')
  } catch (error) {
    setNotice(error.message || '系统配置保存失败，请稍后重试。', 'danger')
  } finally {
    savingAction.value = ''
  }
}

function batchImportUsers() {
  setNotice('批量导入入口已保留，当前演示版不执行真实文件上传。')
}

function maskPhone(phone) {
  const text = String(phone || '')
  if (text.length < 7) return text || '-'
  return `${text.slice(0, 3)}****${text.slice(-4)}`
}

function splitList(value) {
  if (Array.isArray(value)) return value
  return String(value || '').split(',').map((item) => item.trim()).filter(Boolean)
}

function normalizeUploadInterval(value) {
  const text = String(value || '').trim()
  if (['1', '60', '1分钟', '每分钟'].includes(text)) return '60'
  if (['5', '300', '5分钟', '每5分钟'].includes(text)) return '300'
  if (['10', '600', '10分钟', '每10分钟'].includes(text)) return '600'
  if (['30', '1800', '30分钟', '每30分钟'].includes(text)) return '1800'
  return '60'
}

function auditUsername(value, id) {
  const text = String(value || '').trim()
  const map = {
    admin: '王明轩',
    zhangsan: '李晓燕',
    lisi: '陈志强',
    wangwu: '赵美华'
  }
  if (map[text]) return map[text]
  return cleanText(text, DEFAULT_USERS[id % DEFAULT_USERS.length])
}

function toBoolean(value, fallback) {
  if (value === undefined || value === null || value === '') return fallback
  if (typeof value === 'boolean') return value
  return String(value) === 'true'
}

function sensitivityToSlider(value) {
  return { 低: '1', 中: '2', 高: '3' }[value] || String(value || '2')
}

function sliderToSensitivity(value) {
  return { 1: '低', 2: '中', 3: '高' }[String(value)] || '中'
}

function getFarmId() {
  return localStorage.getItem('farmId') || '1'
}

function setNotice(message, tone = 'success') {
  notice.value = { message, tone }
  window.clearTimeout(setNotice.timer)
  setNotice.timer = window.setTimeout(() => {
    notice.value = { message: '', tone: 'success' }
  }, 3000)
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadPermissions()])
  await Promise.all([loadRoles(), loadAuditLogs()])
  await loadConfigs()
})
</script>

<style src="./settings/settings-page.css"></style>
