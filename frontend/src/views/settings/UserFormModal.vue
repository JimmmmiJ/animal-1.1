<template>
<div class="manual38-overlay">
      <section class="manual38-modal manual38-modal--user">
        <button class="manual38-close" @click="$emit('close')">×</button>
        <h3>{{ editingUser ? '编辑用户' : '新增用户' }}</h3>
        <div class="manual38-form-grid">
          <label class="manual38-field">
            <span>用户名</span>
            <input v-model.trim="userForm.username" />
          </label>
          <label class="manual38-field">
            <span>角色</span>
            <select v-model="userForm.roleCode">
              <option v-for="role in roleList" :key="role.code" :value="role.code">{{ role.label }}</option>
            </select>
          </label>
          <label class="manual38-field">
            <span>邮箱</span>
            <input v-model.trim="userForm.email" type="email" />
          </label>
          <label class="manual38-field">
            <span>手机号</span>
            <input v-model.trim="userForm.phone" />
          </label>
          <label class="manual38-field">
            <span>密码</span>
            <input v-model="userForm.password" type="password" />
          </label>
          <label class="manual38-field">
            <span>确认密码</span>
            <input v-model="userForm.confirmPassword" type="password" />
          </label>
          <label class="manual38-field manual38-field--full">
            <span>状态</span>
            <select v-model="userForm.status">
              <option value="enabled">激活</option>
              <option value="disabled">禁用</option>
            </select>
          </label>
          <label class="manual38-field manual38-field--full">
            <span>备注</span>
            <textarea v-model="userForm.remark"></textarea>
          </label>
        </div>
        <div class="manual38-footer">
          <button class="manual38-btn" @click="$emit('close')">取消</button>
          <button class="manual38-btn manual38-btn--primary" :disabled="savingAction === 'user'" @click="$emit('save')">
            {{ savingAction === 'user' ? '保存中...' : '保存用户' }}
          </button>
        </div>
      </section>
    </div>
</template>

<script setup>
defineProps({
  userForm: { type: Object, required: true },
  editingUser: { type: Object, default: null },
  roleList: { type: Array, default: () => [] },
  savingAction: { type: String, default: '' }
})

defineEmits(['close', 'save'])
</script>
