<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

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
  <router-view />
</template>
