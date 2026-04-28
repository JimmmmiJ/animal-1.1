<template>
  <div class="min-h-screen bg-[var(--manual-bg)]">
    <aside class="fixed inset-y-0 left-0 z-20 w-[176px] border-r border-[var(--manual-border)] bg-white">
      <div class="border-b border-[var(--manual-border)] px-4 py-3">
        <p class="text-[13px] font-bold leading-6 text-[var(--manual-primary)]">
          畜牧健康监测与预警管理
          <br />
          平台
        </p>
      </div>

      <nav class="px-2 py-3">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="mb-1 flex rounded-[4px] px-3 py-2 text-[12px] leading-6 text-[var(--manual-text-muted)] transition hover:bg-[#f5f8fe] hover:text-[var(--manual-primary)]"
          :class="{
            'bg-[var(--manual-primary-soft)] font-semibold text-[var(--manual-primary)]': isActive(item.path)
          }"
        >
          {{ item.name }}
        </router-link>
      </nav>
    </aside>

    <main class="ml-[176px] min-h-screen px-5 py-4">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const menuItems = [
  { path: '/health-data', name: '牲畜健康数据实时采集管理' },
  { path: '/breeding', name: '发情期智能识别预警系统' },
  { path: '/disease', name: '疾病早期检测与风险评估' },
  { path: '/behavior', name: '反刍采食行为分析监控' },
  { path: '/dashboard', name: '个性化健康看板可视化' },
  { path: '/alerts', name: '异常告警推送管理' },
  { path: '/settings', name: '用户权限与系统配置管理' }
]

function isActive(path) {
  return route.path === path
}
</script>
