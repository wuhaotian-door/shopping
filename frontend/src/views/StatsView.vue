<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import StatsPanel from './components/StatsPanel.vue'

const router = useRouter()
const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)

onMounted(() => {
  document.title = '数据统计 — 码上非遗'
})
</script>

<template>
  <div class="stats-page-body">
    <StatsPanel v-if="isLoggedIn" />
    <div v-else class="guest-prompt">
      <div class="prompt-card">
        <div class="prompt-icon">📊</div>
        <h2>请先登录</h2>
        <p>登录后即可查看非遗传承人数据统计分析</p>
        <button class="login-btn" @click="router.push('/login')">前往登录</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stats-page-header {
  background: #c41e3a;
  color: #fff;
  padding: 0 5%;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.logo { font-size: 18px; font-weight: bold; cursor: pointer; }
.header-title { font-size: 14px; opacity: 0.85; }
.back-btn {
  padding: 6px 16px; font-size: 13px; color: #fff; background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.4); border-radius: 4px; cursor: pointer;
}
.back-btn:hover { background: rgba(255,255,255,0.3); }
.stats-page-body {
  padding: 32px 5%;
  min-height: calc(100vh - 56px);
  max-width: 1200px;
  margin: 0 auto;
  background: #fcf4e8;
}

.guest-prompt {
  display: flex; align-items: center; justify-content: center;
  min-height: 60vh;
}
.prompt-card {
  text-align: center; background: #fff; border-radius: 16px;
  padding: 60px 50px; box-shadow: 0 4px 20px rgba(139,26,43,0.08);
  max-width: 420px; width: 100%;
}
.prompt-icon { font-size: 56px; margin-bottom: 16px; }
.prompt-card h2 { font-size: 24px; color: #c41e3a; margin: 0 0 10px; }
.prompt-card p { font-size: 15px; color: #888; margin: 0 0 28px; line-height: 1.6; }
.login-btn {
  padding: 12px 40px; font-size: 16px; font-weight: 600; letter-spacing: 2px;
  color: #fff; background: linear-gradient(135deg, #c41e3a, #d43030);
  border: none; border-radius: 8px; cursor: pointer; transition: 0.2s;
  font-family: inherit;
}
.login-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(196,30,58,0.3); }
</style>
