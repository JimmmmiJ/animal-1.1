<template>
<section class="manual38-role-layout">
      <div class="manual38-role-card">
        <h2>角色列表</h2>
        <button
          v-for="role in roleList"
          :key="role.code"
          :class="['manual38-role-item', { active: selectedRoleCode === role.code }]"
          @click="$emit('select', role)"
        >
          <span>
            <b>{{ role.label }}</b>
            <em>{{ role.description }}</em>
          </span>
          <i>{{ role.permissionCount }}项权限</i>
        </button>
      </div>

      <div class="manual38-permission-card">
        <div class="manual38-permission-head">
          <div>
            <h2>权限分配</h2>
            <p>当前角色: {{ selectedRole?.label || '-' }}</p>
          </div>
          <button class="manual38-mini-btn manual38-mini-btn--primary" :disabled="!selectedRole || savingAction === 'permission'" @click="$emit('save')">
            {{ savingAction === 'permission' ? '保存中...' : '保存权限' }}
          </button>
        </div>
        <div class="manual38-permission-body">
          <label class="manual38-checkline manual38-checkline--all">
            <input v-model="permissionDraft.all" type="checkbox" />
            全部权限
          </label>
          <section v-for="module in permissionModules" :key="module.module" class="manual38-permission-module">
            <h3>{{ module.module }}</h3>
            <div class="manual38-permission-grid">
              <label v-for="item in module.items" :key="item.code" class="manual38-checkline">
                <input v-model="permissionDraft.selected" type="checkbox" :value="item.code" :disabled="permissionDraft.all" />
                {{ item.name }}
              </label>
            </div>
          </section>
        </div>
      </div>
    </section>
</template>

<script setup>
defineProps({
  roleList: { type: Array, default: () => [] },
  selectedRoleCode: { type: String, default: '' },
  selectedRole: { type: Object, default: null },
  permissionModules: { type: Array, default: () => [] },
  permissionDraft: { type: Object, required: true },
  savingAction: { type: String, default: '' }
})

defineEmits(['select', 'save'])
</script>
