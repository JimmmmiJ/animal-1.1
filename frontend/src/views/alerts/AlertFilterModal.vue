<template>
<div class="manual37-overlay">
      <section class="manual37-modal manual37-modal--filter">
        <button class="manual37-close" @click="$emit('close')">×</button>
        <h3>筛选</h3>
        <div class="manual37-form-grid">
          <label class="manual37-field">
            <span>处理状态</span>
            <select v-model="filterDraft.status">
              <option value="">全部状态</option>
              <option value="pending">待处理</option>
              <option value="acknowledged">处理中</option>
              <option value="resolved">已解决</option>
            </select>
          </label>
          <label class="manual37-field">
            <span>严重等级</span>
            <select v-model="filterDraft.severity">
              <option value="">全部等级</option>
              <option value="high">紧急</option>
              <option value="medium">高</option>
              <option value="low">中</option>
            </select>
          </label>
        </div>
        <div class="manual37-footer">
          <button class="manual37-btn" @click="$emit('reset')">重置</button>
          <button class="manual37-btn manual37-btn--primary" @click="$emit('apply')">确定</button>
        </div>
      </section>
    </div>
</template>

<script setup>
defineProps({
  filterDraft: { type: Object, required: true }
})

defineEmits(['close', 'reset', 'apply'])
</script>
