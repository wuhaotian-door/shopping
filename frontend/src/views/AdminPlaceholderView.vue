


<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  document.title = '管理后台——码上非遗'
})

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<template>
  <header class="page-header">
    <div class="logo">码上非遗 · 管理后台</div>
    <div class="header-right">
      <span class="user-name">{{ authStore.userInfo?.name }}</span>
      <button class="logout-btn" @click="handleLogout">退出登录</button>
      <a class="back-link" @click="router.push('/')">返回首页</a>
    </div>
  </header>

  <main class="admin-main">
    <div class="placeholder-card">
      <div class="placeholder-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <rect x="3" y="3" width="18" height="18" rx="3"/>
          <line x1="3" y1="9" x2="21" y2="9"/>
          <line x1="9" y1="21" x2="9" y2="9"/>
        </svg>
      </div>
      <h1>管理后台</h1>
      <p>此页面保留给后续开发人员进行开发。</p>
      <p class="sub">功能规划：用户管理、内容审核、数据统计、非遗项目管理等。</p>
    </div>
  </main>

  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
  </footer>
</template>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
.page-header {
  background-color: #c41e3a;
  color: #fff;
  padding: 0 5%;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo {
  font-size: 20px;
  font-weight: bold;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-name {
  font-size: 14px;
}
.logout-btn {
  padding: 6px 14px;
  font-size: 12px;
  color: #fff;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.4);
  border-radius: 4px;
  cursor: pointer;
  transition: 0.2s;
}
.logout-btn:hover {
  background: rgba(255,255,255,0.3);
}
.back-link {
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  text-decoration: underline;
}

.admin-main {
  min-height: calc(100vh - 64px - 72px);
  background: #fcf4e8;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}
.placeholder-card {
  text-align: center;
  background: #fff;
  padding: 56px 48px;
  border-radius: 14px;
  box-shadow: 0 6px 22px rgba(196, 30, 58, 0.12);
  max-width: 500px;
}
.placeholder-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  color: #c41e3a;
}
.placeholder-icon svg {
  width: 100%;
  height: 100%;
}
.placeholder-card h1 {
  font-size: 26px;
  color: #c41e3a;
  margin-bottom: 16px;
}
.placeholder-card p {
  font-size: 15px;
  color: #666;
  margin-bottom: 8px;
}
.placeholder-card .sub {
  font-size: 13px;
  color: #999;
  line-height: 1.6;
}

.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 24px 5%;
}
</style>
