<template>
<section class="manual38-panel">
      <div class="manual38-panel-head">
        <h2>用户列表</h2>
        <div class="manual38-panel-actions">
          <input :value="userKeyword" class="manual38-search" @input="$emit('update:user-keyword', $event.target.value.trim())" placeholder="搜索用户名或邮箱..." />
          <button class="manual38-mini-btn" @click="$emit('batch-import')">批量导入</button>
        </div>
      </div>
      <div class="manual38-table-wrap">
        <table class="manual38-table">
          <thead>
            <tr>
              <th style="width: 8%">头像</th>
              <th style="width: 14%">用户名</th>
              <th style="width: 12%">角色</th>
              <th style="width: 20%">邮箱</th>
              <th style="width: 16%">手机号</th>
              <th style="width: 10%">状态</th>
              <th style="width: 12%">注册时间</th>
              <th style="width: 14%">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>
                <span class="manual38-avatar">{{ user.avatarText }}</span>
              </td>
              <td class="manual38-strong">{{ user.displayName }}</td>
              <td>{{ user.roleLabel }}</td>
              <td>{{ user.email }}</td>
              <td>{{ maskPhone(user.phone) }}</td>
              <td>
                <span class="manual38-status-dot" :class="user.status === 'enabled' ? 'green' : 'gray'"></span>
                {{ user.statusLabel }}
              </td>
              <td>{{ formatDate(user.createdAt) }}</td>
              <td>
                <div class="manual38-row-actions">
                  <button class="manual38-link manual38-link--blue" @click="$emit('edit', user)">编辑</button>
                  <button class="manual38-link manual38-link--green" @click="$emit('reset-password', user)">重置密码</button>
                  <button class="manual38-link manual38-link--red" @click="$emit('delete', user)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="!users.length">
              <td colspan="8" class="manual38-empty">暂无用户数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
</template>

<script setup>
defineProps({
  users: { type: Array, default: () => [] },
  userKeyword: { type: String, default: '' },
  maskPhone: { type: Function, required: true },
  formatDate: { type: Function, required: true }
})

defineEmits(['update:user-keyword', 'batch-import', 'edit', 'reset-password', 'delete'])
</script>
