<template>
<section class="manual38-panel">
      <div class="manual38-panel-head">
        <h2>系统审计日志</h2>
        <div class="manual38-panel-actions">
          <select :value="auditTypeFilter" class="manual38-filter" @change="$emit('update:audit-type-filter', $event.target.value)">
            <option value="">全部类型</option>
            <option v-for="type in auditTypeOptions" :key="type" :value="type">{{ type }}</option>
          </select>
          <input :value="auditDateFrom" class="manual38-date" type="date" @input="$emit('update:audit-date-from', $event.target.value)" />
          <input :value="auditDateTo" class="manual38-date" type="date" @input="$emit('update:audit-date-to', $event.target.value)" />
          <button class="manual38-mini-btn manual38-mini-btn--primary" @click="$emit('filter')">筛选</button>
        </div>
      </div>
      <div class="manual38-table-wrap manual38-table-wrap--audit">
        <table class="manual38-table">
          <thead>
            <tr>
              <th style="width: 18%">时间</th>
              <th style="width: 14%">用户</th>
              <th style="width: 16%">操作类型</th>
              <th style="width: 16%">IP地址</th>
              <th style="width: 26%">操作详情</th>
              <th style="width: 10%">结果</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logs" :key="log.id">
              <td>{{ formatDateTime(log.createdAt) }}</td>
              <td>{{ log.username }}</td>
              <td>{{ log.operationType }}</td>
              <td>{{ log.ipAddress }}</td>
              <td>{{ log.operationDesc }}</td>
              <td>{{ log.result }}</td>
            </tr>
            <tr v-if="!logs.length">
              <td colspan="6" class="manual38-empty">暂无审计日志</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
</template>

<script setup>
defineProps({
  logs: { type: Array, default: () => [] },
  auditTypeOptions: { type: Array, default: () => [] },
  auditTypeFilter: { type: String, default: '' },
  auditDateFrom: { type: String, default: '' },
  auditDateTo: { type: String, default: '' },
  formatDateTime: { type: Function, required: true }
})

defineEmits(['update:audit-type-filter', 'update:audit-date-from', 'update:audit-date-to', 'filter'])
</script>
