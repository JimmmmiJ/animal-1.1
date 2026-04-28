<template>
<div class="manual37-overlay">
      <section class="manual37-modal manual37-modal--batch">
        <button class="manual37-close" @click="$emit('close')">×</button>
        <h3>批量操作</h3>
        <div class="manual37-table-wrap manual37-table-wrap--modal">
          <table class="manual37-table">
            <thead>
              <tr>
                <th style="width: 44px">
                  <input type="checkbox" :checked="allSelected" @change="$emit('toggle-all')" />
                </th>
                <th>告警ID</th>
                <th>牲畜编号</th>
                <th>告警类型</th>
                <th>严重等级</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="event in events" :key="event.alertId">
                <td>
                  <input
                    type="checkbox"
                    :checked="selectedAlertIds.includes(event.alertId)"
                    @change="$emit('toggle', event)"
                  />
                </td>
                <td class="manual37-strong">{{ event.alertId }}</td>
                <td>{{ formatAnimalId(event.animalId) }}</td>
                <td>{{ typeText(event.type) }}</td>
                <td><span class="manual37-severity-label" :class="severityClass(event.severity)">{{ severityText(event.severity) }}</span></td>
                <td><span class="manual37-status-label" :class="statusClass(event.status)">{{ statusText(event.status) }}</span></td>
              </tr>
              <tr v-if="!events.length">
                <td colspan="6" class="manual37-empty">当前列表暂无可批量处理的告警</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="manual37-footer manual37-footer--between">
          <span class="manual37-foot-note">已选择 {{ selectedAlertIds.length }} 条告警</span>
          <div class="manual37-footer-actions">
            <button class="manual37-btn" @click="$emit('close')">取消</button>
            <button class="manual37-btn manual37-btn--orange" :disabled="!selectedAlertIds.length || savingAction === 'batch-ack'" @click="$emit('acknowledge')">
              批量确认
            </button>
            <button class="manual37-btn manual37-btn--green" :disabled="!selectedAlertIds.length || savingAction === 'batch-resolve'" @click="$emit('resolve')">
              批量解决
            </button>
          </div>
        </div>
      </section>
    </div>
</template>

<script setup>
defineProps({
  events: { type: Array, default: () => [] },
  selectedAlertIds: { type: Array, default: () => [] },
  allSelected: { type: Boolean, default: false },
  savingAction: { type: String, default: '' },
  formatAnimalId: { type: Function, required: true },
  typeText: { type: Function, required: true },
  severityClass: { type: Function, required: true },
  severityText: { type: Function, required: true },
  statusClass: { type: Function, required: true },
  statusText: { type: Function, required: true }
})

defineEmits(['close', 'toggle', 'toggle-all', 'acknowledge', 'resolve'])
</script>
