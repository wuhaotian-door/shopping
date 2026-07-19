<script setup lang="ts">
import { onMounted, onErrorCaptured, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const renderError = ref<string>('')

onErrorCaptured((err: unknown) => {
  renderError.value = err instanceof Error ? (err.message + '\n' + err.stack) : String(err)
  console.error('App error captured:', err)
  return false // prevent error propagation
})

onMounted(async () => {
  const authStore = useAuthStore()
  const router = useRouter()
  const hadToken = !!authStore.token

  await authStore.initAuth()

  // token 失效时（如后端重启），仅在需要登录的页面才跳转
  if (hadToken && !authStore.token) {
    const route = router.currentRoute.value
    const needsAuth = route.meta.requiresAdmin || route.meta.requiresInheritor || route.meta.requiresAuth === true
    if (needsAuth) {
      router.push('/login')
    }
  }
})
</script>

<template>
  <div v-if="renderError" style="padding:40px;color:red;font-family:monospace;white-space:pre-wrap;background:#fff;">
    <h2>页面渲染错误</h2>
    {{ renderError }}
  </div>
  <router-view v-else />
</template>
