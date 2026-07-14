<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const countdown = ref(5)
let timer: ReturnType<typeof setInterval> | null = null

function goLogin() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  router.push('/login')
}

onMounted(() => {
  document.title = '注册成功——码上非遗'
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      goLogin()
    }
  }, 1000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<template>
  <header class="page-header">
    <div class="logo">码上非遗</div>
    <a @click="$router.push('/')" class="back-link">返回首页</a>
  </header>

  <div class="success-wrap">
    <div class="success-card">
      <!-- 勾选图标 -->
      <div class="check-circle">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
          <polyline points="20 6 9 17 4 12" />
        </svg>
      </div>

      <h1 class="success-title">注册成功</h1>
      <p class="success-subtitle">
        {{ countdown > 0 ? `${countdown} 秒后自动跳转到登录页面...` : '正在跳转...' }}
      </p>

      <button class="login-btn" @click="goLogin">立即登录</button>
    </div>
  </div>

  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
  </footer>
</template>

<style scoped>
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
.back-link {
  color: #fff;
  cursor: pointer;
}
.back-link:hover {
  text-decoration: underline;
}

.success-wrap {
  min-height: calc(100vh - 64px - 120px);
  background: #fcf4e8;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.success-card {
  width: 100%;
  max-width: 420px;
  padding: 56px 40px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 6px 22px rgba(196, 30, 58, 0.12);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.check-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #c41e3a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}
.check-circle svg {
  width: 40px;
  height: 40px;
}

.success-title {
  font-size: 28px;
  color: #c41e3a;
  margin: 0 0 16px 0;
}

.success-subtitle {
  font-size: 15px;
  color: #888;
  margin: 0 0 32px 0;
}

.login-btn {
  width: 100%;
  height: 46px;
  background: #c41e3a;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.25s;
}
.login-btn:hover {
  background: #a8162b;
}

.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 24px 5%;
}
</style>