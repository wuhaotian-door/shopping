<script setup lang="ts">
import { ref, computed, onMounted, onErrorCaptured } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/AppHeader.vue'
import SplashScreen from '@/components/SplashScreen.vue'
import HeritageBg from '@/components/HeritageBg.vue'
import AiFab from '@/components/AiFab.vue'
import { useHistory } from '@/composables/useHistory'
import { useUnread } from '@/composables/useUnread'

const router = useRouter()
const route = useRoute()
useHistory()
useUnread() // 全局未读消息轮询
const authStore = useAuthStore()

const showSplash = ref(!sessionStorage.getItem('splash_done'))

const noHeaderNames = ['Login', 'Register', 'RegisterSuccess', 'ForgotPassword', 'Admin', 'Inheritor', 'Cart', 'Orders']
const showHeader = computed(() => !noHeaderNames.includes(route.name as string))

/* ===== 朱砂水波过渡 ===== */
const ripplePhase = ref<'idle' | 'emit' | 'spread' | 'done'>('idle')

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
  if (route.name !== 'Login') {
    startRipple()
  }
}

function startRipple() {
  ripplePhase.value = 'emit'   // 红点从太阳位置出现
  setTimeout(() => { ripplePhase.value = 'spread' }, 200)
  setTimeout(() => { ripplePhase.value = 'done'  }, 900)
}
</script>

<template>
  <SplashScreen v-if="showSplash" @done="onSplashDone" />

  <!-- ===== 朱砂水波过渡 ===== -->
  <div v-if="ripplePhase !== 'idle' && ripplePhase !== 'done'" class="ripple-overlay" :class="'ripple-' + ripplePhase">
    <!-- 中心红日原点（太阳位置在页面正上方） -->
    <div class="ripple-center"></div>
    <!-- 水波圈层 -->
    <div class="ripple-ring ring-1"></div>
    <div class="ripple-ring ring-2"></div>
    <div class="ripple-ring ring-3"></div>
  </div>

  <!-- ===== 主内容 ===== -->
  <Transition name="page-enter">
    <div v-if="showContent" class="app-main">
      <HeritageBg />
      <AiFab />
      <AppHeader v-if="showHeader" />
      <div v-if="renderError" style="padding:40px;color:red;font-family:monospace;white-space:pre-wrap;background:#fff;min-height:100vh;">
        <h2>页面渲染错误</h2>
        {{ renderError }}
      </div>
      <div v-else class="page-view" :key="route.fullPath">
        <router-view />
      </div>
    </div>
  </Transition>
</template>

<style>
.page-view { animation: pageIn 0.3s cubic-bezier(0.22,0.61,0.36,1); }
@keyframes pageIn {
  0% { opacity: 0; transform: translateY(12px); }
  100% { opacity: 1; transform: translateY(0); }
}
/* ===== 朱砂水波过渡覆盖层 ===== */
.ripple-overlay {
  position: fixed; inset: 0; z-index: 9998;
  pointer-events: none;
  overflow: hidden;
}

/* 中心红日原点（位于页面正上方，对应太阳最终位置） */
.ripple-center {
  position: absolute;
  top: 4%; left: 50%;
  width: 0; height: 0;
}

/* 水波圈 */
.ripple-ring {
  position: absolute;
  top: 4%; left: 50%;
  width: 20px; height: 20px;
  border-radius: 50%;
  transform: translate(-50%, -50%) scale(0);
  border: 2px solid rgba(196, 30, 58, 0.6);
  background: transparent;
  opacity: 0;
}

/* 发射阶段 */
.ripple-emit .ripple-center {
  width: 12px; height: 12px;
  background: #c41e3a;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  animation: rippleCenterAppear 0.2s ease-out forwards;
}
@keyframes rippleCenterAppear {
  0%   { transform: translate(-50%, -50%) scale(0); opacity: 0; }
  100% { transform: translate(-50%, -50%) scale(1); opacity: 1; }
}

/* 扩散阶段 */
.ripple-spread .ripple-center {
  width: 12px; height: 12px;
  background: #c41e3a;
  border-radius: 50%;
  transform: translate(-50%, -50%) scale(1);
  animation: rippleCenterFade 0.7s ease-out forwards;
}
@keyframes rippleCenterFade {
  0%   { opacity: 1; }
  100% { opacity: 0; transform: translate(-50%, -50%) scale(3); }
}

/* 水波圈依次扩散 */
.ripple-spread .ring-1 {
  animation: rippleOut 0.7s ease-out 0s forwards;
}
.ripple-spread .ring-2 {
  animation: rippleOut 0.7s ease-out 0.15s forwards;
}
.ripple-spread .ring-3 {
  animation: rippleOut 0.7s ease-out 0.3s forwards;
}

@keyframes rippleOut {
  0%   { transform: translate(-50%, -50%) scale(0); opacity: 0.7; }
  40%  { opacity: 0.5; }
  100% { transform: translate(-50%, -50%) scale(200); opacity: 0; }
}

/* ===== 全局内容层 ===== */
.app-main { position: relative; z-index: 1; }

/* ===== 主页入场 ===== */
.page-enter-enter-active {
  transition: opacity 0.5s ease 0.3s, transform 0.5s cubic-bezier(0.22, 0.61, 0.36, 1) 0.3s;
}
.page-enter-leave-active {
  transition: opacity 0.2s ease;
}
.page-enter-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.page-enter-leave-to {
  opacity: 0;
}
</style>
