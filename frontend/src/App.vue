<template>
  <router-view />
  <Transition name="manual-toast">
    <div
      v-if="notice.message"
      :class="[
        'fixed right-5 top-5 z-[80] rounded-[6px] border px-4 py-3 text-[13px] shadow-[0_10px_24px_rgba(30,50,88,0.14)]',
        notice.tone === 'danger'
          ? 'border-rose-200 bg-rose-50 text-rose-700'
          : 'border-emerald-200 bg-emerald-50 text-emerald-700'
      ]"
    >
      {{ notice.message }}
    </div>
  </Transition>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const notice = ref({ message: '', tone: 'success' })
let timer = null

function handleNotice(event) {
  notice.value = {
    message: event.detail?.message || '',
    tone: event.detail?.tone || 'success'
  }
  window.clearTimeout(timer)
  timer = window.setTimeout(() => {
    notice.value = { message: '', tone: 'success' }
  }, event.detail?.duration || 2400)
}

onMounted(() => {
  window.addEventListener('manual-notice', handleNotice)
})

onBeforeUnmount(() => {
  window.removeEventListener('manual-notice', handleNotice)
  window.clearTimeout(timer)
})
</script>
