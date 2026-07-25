<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const isInheritor = computed(() => route.query.type === 'inheritor')

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
  if (isInheritor.value) {
    document.title = '申请已提交——码上非遗'
  } else {
    document.title = '注册成功——码上非遗'
  }
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
    <div class="logo" @click="$router.push('/')">
      <img src="/logo.png" alt="码上非遗" class="logo-img" />
      <span class="logo-text">码上非遗</span>
    </div>
    <a @click="$router.push('/')" class="back-link">返回首页</a>
  </header>

  <div class="success-wrap">
    <div class="success-card">
      <!-- 图标 -->
      <div class="check-circle" :class="{ pending: isInheritor }">
        <svg v-if="!isInheritor" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
          <polyline points="20 6 9 17 4 12" />
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </svg>
      </div>

      <h1 class="success-title">{{ isInheritor ? '申请已提交' : '注册成功' }}</h1>
      <p class="success-subtitle" v-if="isInheritor">
        您的非遗传承人入驻申请已提交，请等待管理员审核通过后再登录。
      </p>
      <p class="success-subtitle" v-else>
        {{ countdown > 0 ? `${countdown} 秒后自动跳转到登录页面...` : '正在跳转...' }}
      </p>

      <button class="login-btn" @click="goLogin">前往登录</button>
    </div>
  </div>

  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
  </footer>
</template>

<style scoped>
.page-header {
  background: linear-gradient(135deg, #8b1a2b 0%, #a01e35 40%, #c41e3a 100%);
  color: #fff;
  padding: 0 6%;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
  font-family: 'Noto Serif SC', 'STKaiti', 'KaiTi', '楷体', 'STSong', serif;
  box-shadow: 0 2px 16px rgba(139, 26, 43, 0.35);
}
.logo {
  display: flex; align-items: center; gap: 10px;
  cursor: pointer; white-space: nowrap;
}
.logo-img { height: 42px; width: auto; }
.logo-text { font-size: 22px; font-weight: 700; letter-spacing: 4px; color: #fff; }
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
.check-circle.pending {
  background: #e67e22;
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
.pending ~ .success-title {
  color: #e67e22;
}

.success-subtitle {
  font-size: 15px;
  color: #888;
  margin: 0 0 32px 0;
  line-height: 1.8;
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

@media (max-width: 768px) {
  .page-header { height: 56px; padding: 0 4%; }
  .logo-img { height: 30px; }
  .logo-text { font-size: 16px; letter-spacing: 2px; }
  .back-link { font-size: 13px; }

  .success-wrap {
    min-height: calc(100vh - 56px - 80px);
    padding: 24px 16px;
    align-items: flex-start;
    padding-top: 48px;
  }

  .success-card {
    max-width: 100%;
    padding: 36px 24px;
    border-radius: 12px;
    box-shadow: 0 4px 16px rgba(196, 30, 58, 0.1);
  }

  .check-circle {
    width: 64px;
    height: 64px;
    margin-bottom: 18px;
  }
  .check-circle svg { width: 32px; height: 32px; }

  .success-title { font-size: 22px; margin-bottom: 12px; }
  .success-subtitle { font-size: 14px; margin-bottom: 24px; line-height: 1.6; }

  .login-btn { height: 44px; font-size: 15px; }

  .page-footer { padding: 18px 4%; font-size: 12px; }
}
</style>
