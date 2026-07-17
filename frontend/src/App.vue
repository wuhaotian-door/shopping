<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

onMounted(async () => {
  const authStore = useAuthStore()
  const router = useRouter()
  const hadToken = !!authStore.token

  await authStore.initAuth()

  // 如果之前有 token 但校验后失效了（后端重启），强制跳转登录页
  if (hadToken && !authStore.token) {
    router.push('/login')
  }
})
</script>

<template>
  <router-view />
</template>
