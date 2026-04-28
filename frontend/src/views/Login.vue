<template>
  <div class="min-h-screen bg-white px-4 py-8">
    <div class="mx-auto max-w-[960px]">
      <div class="flex min-h-[560px] items-center justify-center bg-[linear-gradient(180deg,#eaf4ff_0%,#d8ecff_100%)] px-6 py-10">
        <section class="w-full max-w-[330px] rounded-[6px] border border-[var(--manual-border)] bg-white px-7 py-7 shadow-[0_10px_24px_rgba(42,63,99,0.10)]">
          <div class="text-center">
            <p class="text-[18px] font-bold tracking-[0.02em] text-[#23427e]">畜牧健康监测与预警管理平台</p>
          </div>

          <form class="mt-7 space-y-4" @submit.prevent="handleLogin">
            <label class="manual-field">
              <span class="manual-field-label">用户名</span>
              <input
                v-model="username"
                type="text"
                required
                class="manual-input !h-9"
                placeholder="请输入用户名"
              />
            </label>

            <label class="manual-field">
              <span class="manual-field-label">密码</span>
              <input
                v-model="password"
                type="password"
                required
                class="manual-input !h-9"
                placeholder="请输入密码"
              />
            </label>

            <div class="flex items-center justify-between text-[12px] text-[var(--manual-text-muted)]">
              <label class="flex items-center gap-2">
                <input v-model="rememberMe" type="checkbox" class="h-3.5 w-3.5 rounded border-[var(--manual-border-strong)]" />
                <span>记住我</span>
              </label>
              <a href="#" class="text-[var(--manual-primary)]">忘记密码?</a>
            </div>

            <button type="submit" class="manual-btn manual-btn--primary w-full !h-9 !text-[13px] !font-semibold" :disabled="loading">
              {{ loading ? '登录中...' : '登录' }}
            </button>

            <div
              v-if="errorMsg"
              class="rounded-[4px] border border-[var(--manual-danger)] bg-[var(--manual-danger-soft)] px-3 py-2 text-[12px] text-[var(--manual-danger)]"
            >
              {{ errorMsg }}
            </div>
          </form>

          <div class="mt-5 text-center text-[12px] text-[var(--manual-text-muted)]">
            还没有账号？
            <a href="#" class="font-medium text-[var(--manual-primary)]">立即注册</a>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api'

const router = useRouter()
const username = ref('')
const password = ref('')
const rememberMe = ref(false)
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  loading.value = true
  errorMsg.value = ''

  try {
    const data = await api.auth.login(username.value, password.value)

    if (data.success) {
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
      localStorage.setItem('farmId', '1')
      if (rememberMe.value) {
        localStorage.setItem('rememberedUsername', username.value)
      } else {
        localStorage.removeItem('rememberedUsername')
      }
      router.push('/dashboard')
    } else {
      errorMsg.value = data.message || '登录失败，请检查用户名和密码。'
    }
  } catch (error) {
    errorMsg.value = error.message || '网络异常，请确认后端服务是否已启动。'
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

const remembered = localStorage.getItem('rememberedUsername')
if (remembered) {
  username.value = remembered
  rememberMe.value = true
}
</script>
