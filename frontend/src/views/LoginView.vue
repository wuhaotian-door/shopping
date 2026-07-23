<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import DecoCard from '@/components/DecoCard.vue'

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
    // 按角色跳转：管理员 → /admin，传承人 → /inheritor，普通用户 → /
    if (authStore.userInfo?.role === 'ADMIN') {
      router.push('/admin')
    } else if (authStore.userInfo?.role === 'INHERITOR') {
      router.push('/inheritor')
    } else {
      router.push('/')
    }
  } catch (e: unknown) {
    errorMsg.value = e instanceof Error ? e.message : '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <header class="page-header">
    <div class="logo" @click="$router.push('/')">
      <img src="/logo.png" alt="码上非遗" class="logo-img" />
      <span class="logo-text">码上非遗</span>
    </div>
    <a @click="$router.push('/')" class="back-link">返回首页</a>
  </header>

  <div class="page-wrap">
    <!-- 左侧国风装饰卡片 -->
    <div class="float-left-box">
      <DecoCard
        title="码上非遗"
        subtitle="感受非遗魅力"
        :descLines="['汇聚剪纸、刺绣、陶艺、竹编传统技艺', '连接校园与民间手工艺传承人']"
      />
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

          <div class="form-options">
            <span class="forgot-link" @click="router.push('/forgot-password')">忘记密码？</span>
          </div>

          <button type="submit" class="login-btn" :disabled="loading">
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>

        <div class="switch-tip">
          没有账号？
          <span @click="router.push('/register')" class="register-text">立即注册</span>
        </div>
      </div>
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
  color: rgba(255,255,255,0.85); cursor: pointer; font-size: 15px;
  letter-spacing: 2px; text-decoration: none;
  transition: color 0.2s;
}
.back-link:hover { color: #fff; text-decoration: underline; }

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
.form-options {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}
.forgot-link {
  font-size: 13px;
  color: #c41e3a;
  cursor: pointer;
}
.forgot-link:hover {
  text-decoration: underline;
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