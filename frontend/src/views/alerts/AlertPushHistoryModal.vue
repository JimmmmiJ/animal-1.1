<template>
<div class="manual37-overlay">
      <section class="manual37-modal manual37-modal--push">
        <button class="manual37-close" @click="$emit('close')">×</button>
        <h3>推送历史记录</h3>
        <div class="manual37-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            :class="{ active: filter === tab.value }"
            @click="$emit('update:filter', tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>
        <div class="manual37-table-wrap manual37-table-wrap--modal">
          <table class="manual37-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>接收者</th>
                <th>推送渠道</th>
                <th>告警ID</th>
                <th>状态</th>
                <th>响应时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in items" :key="`${item.id}-${index}`">
                <td>{{ item.pushedAtText }}</td>
                <td>{{ item.receiver }}</td>
                <td>{{ channelText(item.channel) }}</td>
                <td>{{ item.alertId }}</td>
                <td><span class="manual37-status-label" :class="pushStatusClass(item.status)">{{ pushStatusText(item.status) }}</span></td>
                <td>{{ responseTimeText(item, index) }}</td>
              </tr>
              <tr v-if="!items.length">
                <td colspan="6" class="manual37-empty">暂无推送历史</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
</template>

<script setup>
defineProps({
  tabs: { type: Array, default: () => [] },
  filter: { type: String, default: 'all' },
  items: { type: Array, default: () => [] },
  channelText: { type: Function, required: true },
  pushStatusClass: { type: Function, required: true },
  pushStatusText: { type: Function, required: true },
  responseTimeText: { type: Function, required: true }
})

defineEmits(['update:filter', 'close'])
</script>
