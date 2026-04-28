<template>
<section class="manual36-panel manual36-event-panel">
      <div class="manual36-panel-head">
        <h2>近期健康事件</h2>
        <div class="manual36-event-tabs">
          <button :class="{ active: eventFilter === 'all' }" @click="$emit('update:eventFilter', 'all')">全部</button>
          <button :class="{ active: eventFilter === 'warning' }" @click="$emit('update:eventFilter', 'warning')">警告</button>
          <button :class="{ active: eventFilter === 'critical' }" @click="$emit('update:eventFilter', 'critical')">严重</button>
        </div>
      </div>
      <div class="manual36-timeline">
        <button
          v-for="event in events"
          :key="event.alertId"
          class="manual36-event-item"
          @click="$emit('open', event)"
        >
          <i class="manual36-line-dot"></i>
          <i class="manual36-event-dot" :class="eventSeverityClass(event.severity)"></i>
          <span class="manual36-event-title">{{ event.title }}</span>
          <span class="manual36-event-time">{{ event.time }}</span>
          <small>{{ formatAnimalId(event.animalId) }} {{ event.message }}</small>
        </button>
        <div v-if="!events.length" class="manual36-empty">暂无健康事件</div>
      </div>
    </section>
</template>

<script setup>
defineProps({
  events: { type: Array, default: () => [] },
  eventFilter: { type: String, default: 'all' },
  eventSeverityClass: { type: Function, required: true },
  formatAnimalId: { type: Function, required: true }
})

defineEmits(['update:eventFilter', 'open'])
</script>
