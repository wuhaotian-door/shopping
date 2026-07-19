<script setup lang="ts">
import { ref, computed, onMounted, onErrorCaptured } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/AppHeader.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 不需要公共导航栏的页面（精确匹配，避免 /inheritors 误匹配 /inheritor）
const noHeaderNames = ['Login', 'Register', 'RegisterSuccess', 'ForgotPassword', 'Admin', 'Inheritor']
const showHeader = computed(() => !noHeaderNames.includes(route.name as string))

const renderError = ref<string>('')

onErrorCaptured((err: unknown) => {
  renderError.value = err instanceof Error ? (err.message + '\n' + err.stack) : String(err)
  console.error('App error captured:', err)
  return false
})

onMounted(async () => {
  const hadToken = !!authStore.token
  await authStore.initAuth()

  if (hadToken && !authStore.token) {
    const needsAuth = route.meta.requiresAdmin || route.meta.requiresInheritor || route.meta.requiresAuth === true
    if (needsAuth) {
      router.push('/login')
    }
  }
})
</script>

<template>
  <AppHeader v-if="showHeader" />
  <router-view />
</template>
