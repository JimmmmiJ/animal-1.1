<template>
<section class="manual37-panel">
      <div class="manual37-panel-head">
        <h2>告警事件列表</h2>
        <div class="manual37-panel-actions">
          <button class="manual37-mini-btn" @click="$emit('filter')">筛选</button>
          <button class="manual37-mini-btn" @click="$emit('refresh')">刷新</button>
        </div>
      </div>

      <div class="manual37-table-wrap">
        <table class="manual37-table">
          <thead>
            <tr>
              <th style="width: 18%">告警ID</th>
              <th style="width: 14%">牲畜编号</th>
              <th style="width: 14%">告警类型</th>
              <th style="width: 12%">严重等级</th>
              <th style="width: 18%">触发时间</th>
              <th style="width: 10%">状态</th>
              <th style="width: 14%">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="event in events" :key="event.alertId" :class="alertRowClass(event)">
              <td class="manual37-strong">{{ event.alertId }}</td>
              <td>{{ formatAnimalId(event.animalId) }}</td>
              <td><span class="manual37-type-label" :class="typeClass(event.type)">{{ typeText(event.type) }}</span></td>
              <td><span class="manual37-severity-label" :class="severityClass(event.severity)">{{ severityText(event.severity) }}</span></td>
              <td>{{ event.time }}</td>
              <td><span class="manual37-status-label" :class="statusClass(event.status)">{{ statusText(event.status) }}</span></td>
              <td>
                <div class="manual37-row-actions">
                  <button class="manual37-link manual37-link--blue" @click="$emit('detail', event)">详情</button>
                  <button
                    class="manual37-link manual37-link--orange"
                    :disabled="event.status !== 'pending' || savingAction === `ack-${event.alertId}`"
                    @click="$emit('acknowledge', event)"
                  >
                    确认
                  </button>
                  <button
                    class="manual37-link manual37-link--green"
                    :disabled="event.status === 'resolved' || savingAction === `resolve-${event.alertId}`"
                    @click="$emit('resolve', event)"
                  >
                    解决
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!events.length">
              <td colspan="7" class="manual37-empty">暂无告警事件</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
</template>

<script setup>
defineProps({
  events: { type: Array, default: () => [] },
  savingAction: { type: String, default: '' },
  alertRowClass: { type: Function, required: true },
  formatAnimalId: { type: Function, required: true },
  typeClass: { type: Function, required: true },
  typeText: { type: Function, required: true },
  severityClass: { type: Function, required: true },
  severityText: { type: Function, required: true },
  statusClass: { type: Function, required: true },
  statusText: { type: Function, required: true }
})

defineEmits(['filter', 'refresh', 'detail', 'acknowledge', 'resolve'])
</script>
