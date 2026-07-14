<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

onMounted(() => {
  document.title = "码上非遗——感受非遗魅力"
})

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
})

const errorMsg = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    errorMsg.value = '请输入用户名和密码'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    await authStore.login(form.username, form.password)
    router.push('/')
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <header class="page-header">
    <div class="logo">码上非遗</div>
    <a @click="$router.push('/')" class="back-link">返回首页</a>
  </header>

  <div class="page-wrap">
    <!-- 左侧国风灵动互动窗口 -->
    <div class="float-left-box">
      <div
          class="float-card"
          @mousemove="onMouseMove"
          @mouseenter="onMouseEnter"
          @mouseleave="onMouseLeave"
      >
        <!-- 背景流光 -->
        <div class="flow-light"></div>

        <!-- 国风纹样层 -->
        <div class="pattern-layer pattern-1"></div>
        <div class="pattern-layer pattern-2"></div>
        <div class="pattern-layer pattern-3"></div>

        <!-- 浮动装饰 -->
        <div class="float-deco deco1"></div>
        <div class="float-deco deco2"></div>
        <div class="float-deco deco3"></div>

        <!-- 鼠标跟随粒子 -->
        <div class="particle" v-for="i in 6" :key="i" :style="particleStyle(i)"></div>

        <div class="float-text-wrap">
          <h2>码上非遗</h2>
          <p class="slogan">感受非遗魅力</p>
          <p class="desc">汇聚剪纸、刺绣、陶艺、竹编传统技艺<br>连接校园与民间手工艺传承人</p>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="login-right-box">
      <div class="login-card">
        <h1 class="login-title">账号登录</h1>
        <div class="title-line"></div>

        <form class="login-form" @submit.prevent="handleLogin">
          <div class="form-item">
            <label for="username">用户名</label>
            <input
                id="username"
                v-model="form.username"
                type="text"
                placeholder="示例：张三"
                autocomplete="username"
            />
          </div>

          <div class="form-item">
            <label for="password">密码</label>
            <input
                id="password"
                v-model="form.password"
                type="password"
                placeholder="示例：123456"
                autocomplete="current-password"
            />
          </div>

          <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

          <button type="submit" class="login-btn" :disabled="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>

        <div class="switch-tip">
          没有账号？
          <span @click="$Router.push('/register')" class="register-text">立即注册</span>
        </div>
      </div>
    </div>
  </div>

  <footer class="page-footer">
    <p>校园非遗与文创传承人平台 © 2026 码上非遗</p>
  </footer>
</template>

<script lang="ts">
export default {
  methods: {
    onMouseMove(e: MouseEvent) {
      const card = e.currentTarget as HTMLElement
      const rect = card.getBoundingClientRect()
      const x = e.clientX - rect.left - rect.width / 2
      const y = e.clientY - rect.top - rect.height / 2

      card.style.transform = `
        translateY(-6px)
        perspective(1000px)
        rotateY(${x * 0.008}deg)
        rotateX(${-y * 0.008}deg)
      `

      card.style.setProperty('--mouse-x', `${e.clientX - rect.left}px`)
      card.style.setProperty('--mouse-y', `${e.clientY - rect.top}px`)
    },
    onMouseEnter(e: MouseEvent) {
      const card = e.currentTarget as HTMLElement
      card.style.transition = 'transform 0.15s ease-out, box-shadow 0.2s ease'
      card.style.boxShadow = '0 12px 36px rgba(196, 30, 58, 0.35)'
    },
    onMouseLeave(e: MouseEvent) {
      const card = e.currentTarget as HTMLElement
      card.style.transition = 'transform 0.3s ease, box-shadow 0.3s ease'
      card.style.transform = 'translateY(0) perspective(1000px) rotateY(0) rotateX(0)'
      card.style.boxShadow = '0 8px 30px rgba(196, 30, 58, 0.2)'
    },
    particleStyle(index: number) {
      const size = 8 + index * 2
      return {
        width: `${size}px`,
        height: `${size}px`,
        left: `${15 + index * 15}%`,
        animationDelay: `${index * 0.4}s`,
      }
    }
  }
}
</script>

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

.page-wrap {
  min-height: calc(100vh - 64px - 120px);
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #fcf4e8;
}

.float-left-box {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.float-card {
  width: 100%;
  max-width: 480px;
  height: 520px;
  border-radius: 18px;
  background: linear-gradient(145deg, #c41e3a, #e8647a);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 30px rgba(196, 30, 58, 0.2);
  cursor: pointer;
  perspective: 1000px;
  transform-style: preserve-3d;
}

/* 鼠标位置变量 */
.float-card {
  --mouse-x: 50%;
  --mouse-y: 50%;
}

/* 流光效果 */
.flow-light {
  position: absolute;
  width: 120%;
  height: 120%;
  top: -10%;
  left: -10%;
  background: radial-gradient(
      circle at var(--mouse-x, 50%) var(--mouse-y, 50%),
      rgba(255, 255, 255, 0.24),
      transparent 40%
  );
  opacity: 0;
  transition: opacity 0.25s ease;
  pointer-events: none;
}

.float-card:hover .flow-light {
  opacity: 1;
}

/* 国风剪纸/窗花纹样 */
.pattern-layer {
  position: absolute;
  width: 180px;
  height: 180px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  opacity: 0;
  transform: scale(0.8);
  transition: all 0.4s ease;
  pointer-events: none;
}

.pattern-1 {
  top: 30px;
  left: 30px;
  clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%);
}

.pattern-2 {
  bottom: 40px;
  right: 40px;
  width: 140px;
  height: 140px;
  clip-path: polygon(25% 0%, 75% 0%, 100% 50%, 75% 100%, 25% 100%, 0% 50%);
}

.pattern-3 {
  top: 50%;
  left: 50%;
  width: 100px;
  height: 100px;
  transform: translate(-50%, -50%) scale(0.8);
  clip-path: circle(50% at 50% 50%);
}

.float-card:hover .pattern-layer {
  opacity: 0.65;
  transform: scale(1);
}

.float-card:hover .pattern-1 {
  animation: floatMove 8s ease-in-out infinite;
}

.float-card:hover .pattern-2 {
  animation: floatMove 10s ease-in-out infinite reverse;
}

.float-card:hover .pattern-3 {
  animation: pulse 4s ease-in-out infinite;
}

/* 浮动装饰 */
.float-deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
  animation: floatMove 6s ease-in-out infinite;
  pointer-events: none;
}
.deco1 {
  width: 180px;
  height: 180px;
  top: 40px;
  left: 40px;
  animation-delay: 0s;
}
.deco2 {
  width: 120px;
  height: 120px;
  bottom: 80px;
  right: 60px;
  animation-delay: 1.8s;
}
.deco3 {
  width: 70px;
  height: 70px;
  top: 160px;
  right: 120px;
  animation-delay: 3.5s;
}

/* 跟随粒子 */
.particle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.35);
  animation: particleFloat 3s ease-in-out infinite;
  pointer-events: none;
}

@keyframes floatMove {
  0% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-22px) scale(1.08);
  }
  100% {
    transform: translateY(0) scale(1);
  }
}

@keyframes pulse {
  0% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.65;
  }
  50% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.65;
  }
}

@keyframes particleFloat {
  0% {
    transform: translateY(0) translateX(0);
    opacity: 0.35;
  }
  50% {
    transform: translateY(-30px) translateX(8px);
    opacity: 0.7;
  }
  100% {
    transform: translateY(0) translateX(0);
    opacity: 0.35;
  }
}

.float-text-wrap {
  z-index: 2;
  text-align: center;
  color: #fff;
  padding: 0 30px;
}
.float-text-wrap h2 {
  font-size: 42px;
  margin-bottom: 10px;
  letter-spacing: 4px;
}
.slogan {
  font-size: 20px;
  margin-bottom: 24px;
  opacity: 0.9;
}
.desc {
  font-size: 15px;
  line-height: 2;
  opacity: 0.85;
}

.login-right-box {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px 32px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 6px 22px rgba(196, 30, 58, 0.12);
}
.login-title {
  text-align: center;
  font-size: 26px;
  color: #c41e3a;
  margin-bottom: 10px;
}
.title-line {
  width: 50px;
  height: 3px;
  background: #c41e3a;
  margin: 0 auto 32px;
}
.login-form {
  display: flex;
  flex-direction: column;
}
.form-item {
  margin-bottom: 22px;
}
.form-item label {
  display: block;
  font-size: 15px;
  color: #444;
  margin-bottom: 8px;
}
.form-item input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #e8647a;
  border-radius: 8px;
  font-size: 15px;
  color: #333;
  box-sizing: border-box;
  transition: 0.25s;
  outline: none;
}
.form-item input:focus {
  border-color: #c41e3a;
  box-shadow: 0 0 0 3px rgba(196, 30, 58, 0.1);
}
.error-msg {
  color: #c41e3a;
  font-size: 13px;
  margin: 0 0 16px 0;
  text-align: center;
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
.login-btn:hover:not(:disabled) {
  background: #a8162b;
}
.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.switch-tip {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #666;
}
.register-text {
  color: #c41e3a;
  cursor: pointer;
}
.register-text:hover {
  text-decoration: underline;
}

.page-footer {
  background-color: #c41e3a;
  color: #fff;
  text-align: center;
  padding: 24px 5%;
}

@media (max-width: 992px) {
  .page-wrap {
    grid-template-columns: 1fr;
  }
  .float-card {
    height: 360px;
  }
  .float-text-wrap h2 {
    font-size: 32px;
  }
}
@media (max-width: 576px) {
  .float-card {
    height: 300px;
  }
  .float-text-wrap h2 {
    font-size: 26px;
  }
}
</style>