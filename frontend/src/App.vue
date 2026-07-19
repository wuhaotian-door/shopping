<script setup lang="ts">
import { ref, computed, onMounted, onErrorCaptured } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/AppHeader.vue'
import SplashScreen from '@/components/SplashScreen.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 开屏动画：每次会话只播放一次
const showSplash = ref(!sessionStorage.getItem('splash_done'))

// 不需要公共导航栏的页面
const noHeaderNames = ['Login', 'Register', 'RegisterSuccess', 'ForgotPassword', 'Admin', 'Inheritor']
const showHeader = computed(() => !noHeaderNames.includes(route.name as string))

// 主页内容是否显示（用于过渡动画）
const showContent = computed(() => !showSplash.value || route.name === 'Login')

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

function onSplashDone() {
  showSplash.value = false
  sessionStorage.setItem('splash_done', '1')
}
</script>

<template>
  <SplashScreen v-if="showSplash" @done="onSplashDone" />
  <Transition name="page-enter">
    <div v-if="showContent" class="app-main">
      <AppHeader v-if="showHeader" />
      <router-view />
    </div>
  </Transition>
</template>

<style>
/* 主页入场过渡：从下方微微上移 + 淡入 */
.page-enter-enter-active {
  transition: opacity 0.55s ease, transform 0.55s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.page-enter-leave-active {
  transition: opacity 0.25s ease;
}
.page-enter-enter-from {
  opacity: 0;
  transform: translateY(18px);
}
.page-enter-leave-to {
  opacity: 0;
}
</style>
